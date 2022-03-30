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
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.front50.Front50Service;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

@Extension
public class RunMultiplePipelinesTask implements Task {

    private final Front50Service front50Service;
    private final DependentPipelineStarter dependentPipelineStarter;
    private final ExecutionRepository executionRepository;

    public RunMultiplePipelinesTask(Optional<Front50Service> front50Service,
                                    DependentPipelineStarter dependentPipelineStarter,
                                    ExecutionRepository executionRepository)  {
        this.front50Service = front50Service.orElse(null);
        this.dependentPipelineStarter = dependentPipelineStarter;
        this.executionRepository = executionRepository;
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

        Map<String, Stack<App>> newTry = utilityHelper.tryWithStack(apps, mapper, gson);

        String application = (String) (stage.getContext().get("pipelineApplication") != null ? stage.getContext().get("pipelineApplication") : stage.getExecution().getApplication());
        if (front50Service == null) {
            throw new UnsupportedOperationException("Cannot start a stored pipeline, front50 is not enabled. Fix this by setting front50.enabled: true");
        }

        List<Map<String, Object>> pipelines = front50Service.getPipelines(application, false);
        List<PipelineExecution> pipelineExecutions = new LinkedList<>();
        ExecutionStatus returnExecutionStatus = ExecutionStatus.SUCCEEDED;

        //find the bigger stack to then wait on the mainThread until stack finish his executions
        Stack<App> tempStack = new ArrayList<>(newTry.values()).get(0);
        for (int i = 1; newTry.values().size() > i; i++) {
            if (new ArrayList<>(newTry.values()).get(i).size() > tempStack.size()) {
                tempStack = new ArrayList<>(newTry.values()).get(i);
            }
        }

        Thread biggerStackThread = null;
        //trigger pipelines in different Threads
        for (Stack<App> stack : newTry.values()) {
            Runnable triggerThis = () -> triggerThread(stack, gson, stage, pipelines, pipelineExecutions);
            Thread triggerThread = new Thread(triggerThis);
            if (tempStack.equals(stack)) {
                System.out.println(tempStack.size());
                biggerStackThread = triggerThread;
            }
            triggerThread.start();
        }
        biggerStackThread.join();

        if (pipelineExecutions.stream().anyMatch(p -> p.getStatus() != ExecutionStatus.SUCCEEDED)) {
            returnExecutionStatus = pipelineExecutions.stream().map(PipelineExecution::getStatus).filter(status -> status != ExecutionStatus.SUCCEEDED).findFirst().get();
        }

        //if rollback property is true and a pipeline gets terminal rollbacks will be executed for
        //completed success pipelines and Terminal pipelines with artifacts created
        if (apps.isRollbackOnFailure()) {
            if (pipelineExecutions.stream().anyMatch(p -> p.getStatus() == ExecutionStatus.TERMINAL)) {
                doRollbacks(pipelineExecutions, stage, application);
            }
        }

        return TaskResult
                .builder(returnExecutionStatus)
                .outputs(Collections.singletonMap("my_message", "Hola"))
                .build();
    }

    //this will get executed if the triggered pipelines created Artifacts in a stage named Deploy Baseline
    private void doRollbacks(List<PipelineExecution> pipelineExecutions, StageExecution stage, String application) {
        List<Map<String, Object>> finalPipelines = front50Service.getPipelines(application, false);
        System.out.println("inside rollback list size");
        System.out.println(pipelineExecutions.size());
        pipelineExecutions.forEach(pipelineExecution -> {
            StageExecution deployStage = pipelineExecution.getStages().stream().filter(stageExecution -> stageExecution.getName().contains("Deploy")).findFirst().get();
            if (deployStage.getOutputs().get("outputs.createdArtifacts") != null) {
                System.out.println("you need to perform a rollback");
                List<Map<String, Object>> createdArtifacts = (List<Map<String, Object>>) deployStage.getOutputs().get("outputs.createdArtifacts");
                List<Map<String, Object>> manifests = (List<Map<String, Object>>) deployStage.getOutputs().get("manifests");

                Map<String, Object> undoRolloutContext = new HashMap<>();
                undoRolloutContext.put("account", deployStage.getContext().get("deploy.account.name"));
                undoRolloutContext.put("manifestName", manifests.get(0).get("kind") + " " + createdArtifacts.get(0).get("name"));
                undoRolloutContext.put("location", createdArtifacts.get(0).get("location"));
                undoRolloutContext.put("numRevisionsBack", 1);
                undoRolloutContext.put("cloudProvider", "kubernetes");
                undoRolloutContext.put("mode", "static");
                undoRolloutContext.put("name", "Undo Rollout (Manifest) for " + createdArtifacts.get(0).get("name"));
                undoRolloutContext.put("refId", "1");
                undoRolloutContext.put("requisiteStageRefIds", new ArrayList<>());
                undoRolloutContext.put("type", "undoRolloutManifest");

                Map<String, Object> rollbackOnFailurePipeline = new HashMap<>();
                rollbackOnFailurePipeline.put("triggers", new ArrayList<>());
                rollbackOnFailurePipeline.put("stages", Arrays.asList(undoRolloutContext));
                rollbackOnFailurePipeline.put("application", application);
                rollbackOnFailurePipeline.put("index", finalPipelines.size());
                rollbackOnFailurePipeline.put("name", "rollbackOnFailure");
                rollbackOnFailurePipeline.put("keepWaitingPipelines", false);
                rollbackOnFailurePipeline.put("limitConcurrent", false);
                rollbackOnFailurePipeline.put("spelEvaluator", "v4");

                if (!finalPipelines.stream().anyMatch(p -> p.get("name").equals("rollbackOnFailure"))) {
                    System.out.println("creating pipeline");
                    front50Service.savePipeline(rollbackOnFailurePipeline, true);
                } else if (!finalPipelines.stream().filter(p -> p.get("name").equals("rollbackOnFailure"))
                        .anyMatch(filterP -> filterP.get("stages").hashCode() == Arrays.asList(undoRolloutContext).hashCode())) {
                    System.out.println("updating pipeline");
                    String id = finalPipelines.stream().filter(p -> p.get("name").equals("rollbackOnFailure")).findFirst().get().get("id").toString();
                    front50Service.updatePipeline(id, rollbackOnFailurePipeline);
                }

                try {
                    Thread.sleep(7000);
                    List<Map<String, Object>> filterPipelinesList = front50Service.getPipelines(application, false);
                    Map<String, Object> triggerThisRollback = filterPipelinesList.stream().filter(p -> p.get("name").equals("rollbackOnFailure")).findAny().get();
                    dependentPipelineStarter.trigger(
                            triggerThisRollback,
                            stage.getExecution().getAuthentication().getUser(),
                            stage.getExecution(),
                            new HashMap(),
                            stage.getId(),
                            stage.getExecution().getAuthentication()
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void triggerThread(Stack<App> stack, Gson gson, StageExecution stage, List<Map<String, Object>> pipelines, List<PipelineExecution> pipelineExecutions) {
        int i = 0;
        while (stack.size() > i) {
            App app = stack.pop();
            Map<String, Object> pipelineConfig = pipelines.stream().filter(pipeline -> pipeline.get("name").equals(app.getChildPipeline())).findFirst().orElse(null);
            String jsonString = gson.toJson(pipelineConfig);
            Type type = new TypeToken<HashMap<String, Object>>() {}.getType();
            Map<String, Object> pipelineConfigCopy = gson.fromJson(jsonString, type);
            TriggerInOrder triggerInOrder = new TriggerInOrder(
                    pipelineConfigCopy,
                    stage,
                    app,
                    dependentPipelineStarter,
                    executionRepository);
            triggerInOrder.run();
            if (!pipelineExecutions.stream().anyMatch(p -> p.getTrigger().getParameters().get("app").equals(
                    triggerInOrder.getPipelineExecution().getTrigger().getParameters().get("app")))) {
                pipelineExecutions.add(triggerInOrder.getPipelineExecution());
            }
            if (triggerInOrder.getPipelineExecution().getStatus() != ExecutionStatus.SUCCEEDED) {
                stack.removeAllElements();
                break;
            }
        }
    }

}
