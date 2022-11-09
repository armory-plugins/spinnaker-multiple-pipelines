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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import com.netflix.spinnaker.orca.front50.Front50Service;

import javax.annotation.Nonnull;

import io.armory.plugin.smp.parseyml.App;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RunMultiplePipelinesTask implements Task {

    private final Front50Service front50Service;
    private final DependentPipelineStarter dependentPipelineStarter;
    private final ObjectMapper objectMapper;

    public RunMultiplePipelinesTask(Optional<Front50Service> front50Service,
                                    DependentPipelineStarter dependentPipelineStarter, ObjectMapper objectMapper)  {
        this.front50Service = front50Service.orElse(null);
        this.dependentPipelineStarter = dependentPipelineStarter;
        this.objectMapper = objectMapper;
    }

    private final Logger logger = LoggerFactory.getLogger(RunMultiplePipelinesTask.class);

    @SneakyThrows
    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        List<List<App>> orderOfExecutions = objectMapper.readValue(objectMapper.writeValueAsString(stage.getContext().get("orderOfExecutions")), new TypeReference<>() {});
        List<App> executionForThisLevel = orderOfExecutions.get((Integer) stage.getContext().get("levelNumber"));

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

        //Add executionIdentifier parameter for UI
        for (App app : executionForThisLevel) {
            app.getArguments().put("executionIdentifier", app.getYamlIdentifier());
        }

        //trigger pipelines in different Threads
        for (App app : executionForThisLevel) {
            Runnable triggerThis = () -> triggerThread(app, stage, pipelines, pipelineExecutions);
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

        if (ObjectUtils.isNotEmpty(stage.getOutputs().get("failureMessage"))) {
            if(pipelineExecutions.isEmpty()) {
                return TaskResult
                        .builder(ExecutionStatus.TERMINAL)
                        .context(stage.getContext())
                        .build();
            }
        }

        if (pipelineExecutions.isEmpty())  {
            logger.warn("pipelineExecutions list is empty... couldn't trigger any child pipelines");
            stage.appendErrorMessage("Child Pipelines were not executed");
            return TaskResult
                    .builder(ExecutionStatus.TERMINAL)
                    .context(stage.getContext())
                    .build();
        }

        logger.info("Returning TaskResult SUCCEEDED for RunMultiplePipelinesTask");
        stage.getContext().put("executionIds", pipelineExecutionsIds);
        stage.getContext().put("orderOfExecutionsSize", orderOfExecutions.size());
        return TaskResult
                .builder(returnExecutionStatus)
                .context(stage.getContext())
                .build();
    }

    @SneakyThrows
    private void triggerThread(App app, StageExecution stage, List<Map<String, Object>> pipelines,
                               List<PipelineExecution> pipelineExecutions) {
        Map<String, Object> pipelineConfig = pipelines.stream().filter(pipeline -> pipeline.get("name").equals(app.getChildPipeline())).findFirst().orElse(null);
        logger.info("Getting pipelineConfig of childPipeline: " + pipelineConfig.get("name"));
        Map<String, Object> pipelineConfigCopy = objectMapper.readValue(objectMapper.writeValueAsString(pipelineConfig), new TypeReference<>() {});
        TriggerInOrder triggerInOrder = new TriggerInOrder(
                pipelineConfigCopy,
                stage,
                app,
                dependentPipelineStarter);
        triggerInOrder.run();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Adding child pipeline execution to pipelineExecutions list... {}", triggerInOrder.getPipelineExecution());
        if (ObjectUtils.isNotEmpty(triggerInOrder.getPipelineExecution())) {
            pipelineExecutions.add(triggerInOrder.getPipelineExecution());
        }
        logger.info("TriggerThread method completed pipelineExecutions list size is " + pipelineExecutions.size());
    }
}
