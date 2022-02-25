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

import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import io.armory.plugin.smp.config.RunMultiplePipelinesConfig;
import io.armory.plugin.smp.config.RunMultiplePipelinesContext;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Extension
public class RunMultiplePipelinesTask implements Task {

    private RunMultiplePipelinesConfig config;

    public RunMultiplePipelinesTask(final RunMultiplePipelinesConfig config)  {
        this.config = config;
    }

    private Object logger = LoggerFactory.getLogger(RunMultiplePipelinesTask.class);

    @NotNull
    @Override
    public TaskResult execute(@NotNull StageExecution stage) {
        Object context = stage.mapTo(RunMultiplePipelinesContext.class);
        String message = config.getMessage();
        return TaskResult
                .builder(ExecutionStatus.SUCCEEDED)
                .outputs(Collections.singletonMap("message", message))
                .build();
    }
}
