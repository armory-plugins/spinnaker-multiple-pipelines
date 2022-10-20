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
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.Trigger;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import com.netflix.spinnaker.orca.jackson.OrcaObjectMapper;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import io.armory.plugin.smp.parseyml.App;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@Getter
public class TriggerInOrder implements Runnable{

    private final Logger logger = LoggerFactory.getLogger(TriggerInOrder.class);

    private final Map<String, Object> pipelineConfigCopy;
    private final StageExecution stage;
    private final App app;
    private final DependentPipelineStarter dependentPipelineStarter;
    private PipelineExecution pipelineExecution;

    public TriggerInOrder(Map<String, Object> pipelineConfigCopy, StageExecution stage, App app, DependentPipelineStarter dependentPipelineStarter) {
        this.pipelineConfigCopy = pipelineConfigCopy;
        this.stage = stage;
        this.app = app;
        this.dependentPipelineStarter = dependentPipelineStarter;
    }

    @SneakyThrows
    @Override
    public void run() {
        ObjectMapper objectOrcaMapper = OrcaObjectMapper.getInstance();
        PipelineExecution pipelineExecutionCopy = objectOrcaMapper.readValue(objectOrcaMapper.writeValueAsString(stage.getExecution()), PipelineExecution.class);
        pipelineExecutionCopy.getStages().clear();
        pipelineExecutionCopy.getStages().add(stage.getExecution().getStages().stream()
                .filter(s -> s.getId().equals(stage.getId())).findFirst().get());

        /*
        Create a Trigger with one extra property "executionIdentifier" to identify the child execution
        executionIdentifier property is under trigger.parentExecution.trigger
        Check StageExecutionsDetails for an example where is used deck-run-multiple-pipelines/src/RunMultiplePipelinesStageExecutionDetails.tsx
        */
        Map modifiedTrigger = objectOrcaMapper.readValue(objectOrcaMapper.writeValueAsString(pipelineExecutionCopy.getTrigger()), Map.class);
        modifiedTrigger.put("executionIdentifier", app.getArguments().remove("executionIdentifier"));
        Trigger trigger = objectOrcaMapper.readValue(objectOrcaMapper.writeValueAsString(modifiedTrigger), Trigger.class);
        pipelineExecutionCopy.setTrigger(trigger);

        try {
            this.pipelineExecution = dependentPipelineStarter.trigger(
                    pipelineConfigCopy,
                    stage.getExecution().getAuthentication().getUser(),
                    pipelineExecutionCopy,
                    app.getArguments(),
                    stage.getId(),
                    getUser(stage.getExecution())
            );
        } catch (Throwable e) {
            logger.error("Entering try catch message {} ", e.getMessage());
            stage.appendErrorMessage(e.getMessage());
            stage.getOutputs().put("failureMessage", e.getMessage());
            return;
        }
        logger.info("Execution status of child pipeline " + app.getArguments().get("app") + " : {}", pipelineExecution.getStatus());
    }

    private PipelineExecution.AuthenticationDetails getUser(PipelineExecution parentPipeline) {
        Optional<String> korkUsername = AuthenticatedRequest.getSpinnakerUser();
        if (korkUsername.isPresent()) {
            String korkAccounts = AuthenticatedRequest.getSpinnakerAccounts().orElse("");
            return new PipelineExecution.AuthenticationDetails(korkUsername.get(), korkAccounts.split(","));
        }

        if (parentPipeline.getAuthentication().getUser() != null) {
            return parentPipeline.getAuthentication();
        }

        return null;
    }

}
