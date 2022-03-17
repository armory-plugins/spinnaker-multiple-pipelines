/*
 * Copyright 2020 Armory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.armory.plugin.smp.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netflix.spectator.api.Registry;
import com.netflix.spinnaker.kork.core.RetrySupport;
import com.netflix.spinnaker.kork.dynamicconfig.DynamicConfigService;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.clouddriver.KatoService;
import com.netflix.spinnaker.orca.clouddriver.OortService;
import com.netflix.spinnaker.orca.clouddriver.pipeline.manifest.UndoRolloutManifestStage;
import com.netflix.spinnaker.orca.clouddriver.tasks.MonitorKatoTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.manifest.UndoRolloutManifestTask;
import com.netflix.spinnaker.orca.clouddriver.tasks.manifest.WaitForManifestStableTask;
import com.netflix.spinnaker.orca.front50.Front50Service;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import com.netflix.spinnaker.orca.pipeline.model.PipelineExecutionImpl;
import com.netflix.spinnaker.orca.pipeline.model.StageExecutionImpl;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
import io.armory.plugin.smp.config.RunMultiplePipelinesConfig;
import io.armory.plugin.smp.config.RunMultiplePipelinesContext;
import javax.annotation.Nonnull;

import io.armory.plugin.smp.config.UtilityHelper;
import io.armory.plugin.smp.parseyml.App;
import io.armory.plugin.smp.parseyml.Apps;
import lombok.SneakyThrows;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Extension
public class RunMultiplePipelinesTask implements Task {

    private final Front50Service front50Service;
    private final DependentPipelineStarter dependentPipelineStarter;
    private final RunMultiplePipelinesConfig config;
    private final ExecutionRepository executionRepository;

    private final OortService oortService;
    private final Registry registry;
    private final KatoService kato;
    private final DynamicConfigService dynamicConfigService;
    private final RetrySupport retrySupport;

    public RunMultiplePipelinesTask(final RunMultiplePipelinesConfig config, Optional<Front50Service> front50Service,
                                    DependentPipelineStarter dependentPipelineStarter, ExecutionRepository executionRepository, OortService oortService, Registry registry, KatoService kato, DynamicConfigService dynamicConfigService, RetrySupport retrySupport)  {
        this.config = config;
        this.front50Service = front50Service.orElse(null);
        this.dependentPipelineStarter = dependentPipelineStarter;
        this.executionRepository = executionRepository;
        this.oortService = oortService;
        this.registry = registry;
        this.kato = kato;
        this.dynamicConfigService = dynamicConfigService;
        this.retrySupport = retrySupport;
    }

    private final Logger logger = LoggerFactory.getLogger(RunMultiplePipelinesTask.class);

    @SneakyThrows
    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        RunMultiplePipelinesContext context = stage.mapTo(RunMultiplePipelinesContext.class);
        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper();
        UtilityHelper utilityHelper = new UtilityHelper();

        Apps apps = utilityHelper.getApps(context, gson, mapper);

        List<App> appOrder = new LinkedList<>();
        List<String> triggerOrder = utilityHelper.getTriggerOrder(apps, appOrder, gson, mapper);
        utilityHelper.sortAppOrderToTriggerOrder(appOrder, triggerOrder);

        String application = (String) (stage.getContext().get("pipelineApplication") != null ? stage.getContext().get("pipelineApplication") : stage.getExecution().getApplication());
        if (front50Service == null) {
            throw new UnsupportedOperationException("Cannot start a stored pipeline, front50 is not enabled. Fix this by setting front50.enabled: true");
        }

        List<Map<String, Object>> pipelines = front50Service.getPipelines(application, false);
        List<Map<String, Object>> pipelineConfigs = utilityHelper.getPipelineConfigs(appOrder, pipelines);
        List<PipelineExecution> pipelineExecutions = new LinkedList<>();
        ExecutionStatus returnExecutionStatus = ExecutionStatus.SUCCEEDED;

        for (int i = 0; i < pipelineConfigs.size(); i++) {
            String jsonString = gson.toJson(pipelineConfigs.get(i));
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            Map<String, Object> pipelineConfigCopy = gson.fromJson(jsonString, type);
            TriggerInOrder triggerInOrder = new TriggerInOrder(
                    pipelineConfigCopy,
                    stage,
                    appOrder.get(i),
                    dependentPipelineStarter,
                    executionRepository);
            triggerInOrder.run();
            pipelineExecutions.add(triggerInOrder.getPipelineExecution());
            if (triggerInOrder.getPipelineExecution().getStatus() != ExecutionStatus.SUCCEEDED) {
                returnExecutionStatus = triggerInOrder.getPipelineExecution().getStatus();
                break;
            }
        }

        if (apps.isRollbackOnFailure()) {
            pipelineExecutions.forEach(pipelineExecution -> {
                if (pipelineExecution.getStatus() == ExecutionStatus.TERMINAL) {
                    StageExecution deployStage = pipelineExecution.getStages().stream().filter(stageExecution -> stageExecution.getName().contains("Deploy Baseline")).findFirst().get();
                    if (deployStage.getOutputs().get("outputs.createdArtifacts") != null) {
                        System.out.println("you need to perform a rollback");
                        List<Map<String, Object>> createdArtifacts = (List<Map<String, Object>>) deployStage.getOutputs().get("outputs.createdArtifacts");
                        List<Map<String, Object>> manifests = (List<Map<String, Object>>) deployStage.getOutputs().get("manifests");

                        UndoRolloutManifestStage undoRolloutManifestStage = new UndoRolloutManifestStage();
                        Map<String, Object> undoRolloutContext = new HashMap<>();

//                        undoRolloutContext.put("manifest.account.name", deployStage.getContext().get("deploy.account.name"));
                        undoRolloutContext.put("account", deployStage.getContext().get("deploy.account.name"));
                        undoRolloutContext.put("manifestName", manifests.get(0).get("kind") + " " + createdArtifacts.get(0).get("name"));
                        undoRolloutContext.put("location", createdArtifacts.get(0).get("location"));
                        undoRolloutContext.put("numRevisionsBack", 1);

                        StageExecution undoStage = new StageExecutionImpl(new PipelineExecutionImpl(ExecutionType.PIPELINE, application), "Undo Rollout (Manifest)", undoRolloutContext);
                        UndoRolloutManifestTask undoRolloutManifestTask = new UndoRolloutManifestTask();
                        undoRolloutManifestTask.execute(undoStage);
                        MonitorKatoTask monitorKatoTask = new MonitorKatoTask(kato, registry, dynamicConfigService, retrySupport);
                        monitorKatoTask.execute(undoStage);
                        WaitForManifestStableTask waitForManifestStableTask = new WaitForManifestStableTask(oortService);
                        waitForManifestStableTask.execute(undoStage);
                    }
                }
            });
        }

        return TaskResult
                .builder(returnExecutionStatus)
                .outputs(Collections.singletonMap("my_message", "Hola"))
                .build();
    }

}
