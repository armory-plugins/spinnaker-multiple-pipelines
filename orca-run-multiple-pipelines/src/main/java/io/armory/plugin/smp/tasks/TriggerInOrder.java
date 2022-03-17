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

import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.front50.DependentPipelineStarter;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionNotFoundException;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
import com.netflix.spinnaker.security.AuthenticatedRequest;
import io.armory.plugin.smp.parseyml.App;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Optional;

@Getter
public class TriggerInOrder implements Runnable{

    private final Map<String, Object> pipelineConfigCopy;
    private final StageExecution stage;
    private final App app;
    private final DependentPipelineStarter dependentPipelineStarter;
    private final ExecutionRepository executionRepository;
    private PipelineExecution pipelineExecution;

    public TriggerInOrder(Map<String, Object> pipelineConfigCopy, StageExecution stage, App app, DependentPipelineStarter dependentPipelineStarter, ExecutionRepository executionRepository) {
        this.pipelineConfigCopy = pipelineConfigCopy;
        this.stage = stage;
        this.app = app;
        this.dependentPipelineStarter = dependentPipelineStarter;
        this.executionRepository = executionRepository;
    }


    @SneakyThrows
    @Override
    public void run() {
        PipelineExecution pipelineExecution = dependentPipelineStarter.trigger(
                pipelineConfigCopy,
                stage.getExecution().getAuthentication().getUser(),
                stage.getExecution(),
                app.getArguments(),
                stage.getId(),
                getUser(stage.getExecution())
        );
            while (true) {
                try {
                    PipelineExecution pipelineExecutionUpdated = executionRepository.retrieve(ExecutionType.PIPELINE, pipelineExecution.getId());
                    if (pipelineExecutionUpdated != null) {
                        if (pipelineExecutionUpdated.getStatus().isComplete()) {
                            this.pipelineExecution = pipelineExecutionUpdated;
                            break;
                        }
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionNotFoundException e) {
                    e.printStackTrace();
                }
            }
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
