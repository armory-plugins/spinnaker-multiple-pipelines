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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import com.netflix.spinnaker.orca.front50.Front50Service;

import javax.annotation.Nonnull;

import io.armory.plugin.smp.parseyml.App;
import io.armory.plugin.smp.parseyml.Apps;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RunMultiplePipelinesTask implements Task {

    static List<PipelineExecution> PIPELINES_EXECUTIONS;
    static List<String> PIPELINES_EXECUTIONSIDS;

    private final Front50Service front50Service;
    private final DependentPipelineStarter dependentPipelineStarter;

    public RunMultiplePipelinesTask( Optional<Front50Service> front50Service,
                                     DependentPipelineStarter dependentPipelineStarter)  {
        this.front50Service = front50Service.orElse(null);
        this.dependentPipelineStarter = dependentPipelineStarter;
    }

    private final Logger logger = LoggerFactory.getLogger(RunMultiplePipelinesTask.class);

    @SneakyThrows
    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        Gson gson = new Gson();
        Apps apps = ParsePipelinesYamlTask.getApps();
        Map<String, Stack<App>> stackApps = ParsePipelinesYamlTask.getStackApps();

        String application = (String) (stage.getContext().get("pipelineApplication") != null ? stage.getContext().get("pipelineApplication") : stage.getExecution().getApplication());
        if (front50Service == null) {
            logger.error("Front50Service not enable error");
            throw new UnsupportedOperationException("Cannot start a stored pipeline, front50 is not enabled. Fix this by setting front50.enabled: true");
        }

        List<Map<String, Object>> pipelines = front50Service.getPipelines(application, false);
        List<PipelineExecution> pipelineExecutions = new LinkedList<>();
        List<String> pipelineExecutionsIds = new ArrayList<>();
        ExecutionStatus returnExecutionStatus = ExecutionStatus.SUCCEEDED;

        //create a linked list of Threads to join on main thread
        List<Thread> threadList = new LinkedList<>();

        //trigger pipelines in different Threads
        AtomicBoolean statusTerminal = new AtomicBoolean(false);
        for (Stack<App> stack : stackApps.values()) {
            Runnable triggerThis = () -> triggerThread(stack, gson, stage, pipelines, pipelineExecutions, statusTerminal);
            threadList.add(new Thread(triggerThis));
        }

        logger.info("Starting " + threadList.size() +  " Threads...");
        threadList.forEach(Thread::start);

        threadList.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        logger.info("Finished calling trigger");

        pipelineExecutions.forEach(p -> pipelineExecutionsIds.add(p.getId()));


        //if rollback property is true and a pipeline gets terminal rollbacks will be executed for
        //completed success pipelines and Terminal pipelines with artifacts created
        if (apps.isRollbackOnFailure()) {
            if (pipelineExecutions.stream().anyMatch(p -> p.getStatus() == ExecutionStatus.TERMINAL)) {
                doRollbacks(pipelineExecutions, stage, application);
            }
        }

        if (ObjectUtils.isNotEmpty(stage.getOutputs().get("failureMessage"))) {
            if(pipelineExecutions.isEmpty()) {
                return TaskResult
                        .builder(ExecutionStatus.TERMINAL)
                        .build();
            }
        }

        if (pipelineExecutions.isEmpty())  {
            logger.warn("pipelineExecutions list is empty... couldn't trigger any child pipelines");
            stage.appendErrorMessage("Child Pipelines were not executed");
            return TaskResult
                    .builder(ExecutionStatus.TERMINAL)
                    .build();
        }

        logger.info("Returning TaskResult everything worked correctly");
        PIPELINES_EXECUTIONS = pipelineExecutions;
        PIPELINES_EXECUTIONSIDS = pipelineExecutionsIds;
        stage.getContext().put("executionIds", pipelineExecutionsIds);
        return TaskResult
                .builder(returnExecutionStatus)
                .build();
    }

    public static List<PipelineExecution> getPipelinesExecutions() {
        return PIPELINES_EXECUTIONS;
    }

    public static List<String> getPipelinesExecutionsids() {
        return PIPELINES_EXECUTIONSIDS;
    }

    //this will get executed if the triggered pipelines created Artifacts in a stage named Deploy Baseline
    private void doRollbacks(List<PipelineExecution> pipelineExecutions, StageExecution stage, String application) {
        List<Map<String, Object>> finalPipelines = front50Service.getPipelines(application, false);
        pipelineExecutions.forEach(pipelineExecution -> {
            StageExecution deployStage = pipelineExecution.getStages().stream().filter(stageExecution -> stageExecution.getName().contains("Deploy")).findFirst().orElseThrow(() -> {
                return new UnsupportedOperationException("No staged named Deploy found to execute automatic rollback, set rollback_onfailure to false");
            });
            if (deployStage.getOutputs().get("outputs.createdArtifacts") != null) {
                List<Map<String, Object>> createdArtifacts = (List<Map<String, Object>>) deployStage.getOutputs().get("outputs.createdArtifacts");
                List<Map<String, Object>> manifests = (List<Map<String, Object>>) deployStage.getOutputs().get("manifests");

                Map<String, Object> undoRolloutContext = new HashMap<>();
                undoRolloutContext.put("account", deployStage.getContext().get("deploy.account.name"));
                undoRolloutContext.put("manifestName", manifests.get(0).get("kind") + " " + createdArtifacts.get(0).get("name"));
                undoRolloutContext.put("location", createdArtifacts.get(0).get("location"));
                undoRolloutContext.put("numRevisionsBack", 1);
                undoRolloutContext.put("cloudProvider", "kubernetes");
                undoRolloutContext.put("mode", "static");
                undoRolloutContext.put("name", "Undo Rollout (Manifest) " + pipelineExecution.getTrigger().getParameters().get("app"));
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
                rollbackOnFailurePipeline.put("parameterConfig", new ArrayList<>());


                if (!finalPipelines.stream().anyMatch(p -> p.get("name").equals("rollbackOnFailure"))) {
                    throw new UnsupportedOperationException("Pipeline \"rollbackOnFailure\" not found create a pipeline with that name");
                } else {
                    String id = finalPipelines.stream().filter(p -> p.get("name").equals("rollbackOnFailure")).findFirst().get().get("id").toString();
                    rollbackOnFailurePipeline.put("id", id);
                    HttpClient httpClient = HttpClient.newBuilder().build();
                    HttpRequest updatePipelineRequest = HttpRequest.newBuilder(URI.create(front50Service.updatePipeline(id, rollbackOnFailurePipeline).getUrl())).build();
                    try {
                        HttpResponse<String> response = httpClient.send(updatePipelineRequest, HttpResponse.BodyHandlers.ofString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
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
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void triggerThread(Stack<App> stack, Gson gson, StageExecution stage, List<Map<String, Object>> pipelines,
                               List<PipelineExecution> pipelineExecutions, AtomicBoolean statusTerminal) {
        int i = 0;
        while (stack.size() > i && !statusTerminal.get()) {
            App app = stack.pop();
            Map<String, Object> pipelineConfig = pipelines.stream().filter(pipeline -> pipeline.get("name").equals(app.getChildPipeline())).findFirst().orElse(null);
            logger.info("Getting pipelineConfig of childPipeline: " + pipelineConfig.get("name"));
            String jsonString = gson.toJson(pipelineConfig);
            Type type = new TypeToken<HashMap<String, Object>>() {}.getType();
            Map<String, Object> pipelineConfigCopy = gson.fromJson(jsonString, type);
            TriggerInOrder triggerInOrder = new TriggerInOrder(
                    pipelineConfigCopy,
                    stage,
                    app,
                    dependentPipelineStarter);
            triggerInOrder.run();
            if (!pipelineExecutions.stream().anyMatch(p -> p.getTrigger().getParameters().get("app").equals(
                    triggerInOrder.getPipelineExecution().getTrigger().getParameters().get("app")))) {
                logger.info("Adding child pipeline execution to pipelineExecutions list... {}", triggerInOrder.getPipelineExecution());
                pipelineExecutions.add(triggerInOrder.getPipelineExecution());
            }
            if (triggerInOrder.getPipelineExecution().getStatus() != ExecutionStatus.SUCCEEDED) {
                if (triggerInOrder.getPipelineExecution().getStatus() == ExecutionStatus.TERMINAL) {
                    statusTerminal.set(true);
                }
                stack.removeAllElements();
                break;
            }
        }
        logger.info("TriggerThread method completed pipelineExecutions list size is " + pipelineExecutions.size());
    }
}
