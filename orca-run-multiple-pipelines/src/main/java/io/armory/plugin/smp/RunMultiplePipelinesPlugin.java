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

package io.armory.plugin.smp;

import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import io.armory.plugin.smp.tasks.RunMultiplePipelinesTask;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class RunMultiplePipelinesPlugin extends Plugin {
    /**
     * Constructor to be used by plugin manager for plugin instantiation.
     * Your plugins have to provide constructor with this exact signature to
     * be successfully loaded by manager.
     *
     * @param wrapper
     */
    public RunMultiplePipelinesPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        log.info("Starting RunMultiplePipelines plugin...");
    }

    @Override
    public void stop() {
        log.info("Stopping RunMultiplePipelines plugin...");
    }
}

@Extension
class RunMultiplePipelinesStage implements StageDefinitionBuilder {
    @Override
    public void taskGraph(@NotNull StageExecution stage, @NotNull TaskNode.Builder builder) {
        builder.withTask("runMultiplePipelinesTask", RunMultiplePipelinesTask.class);
        StageDefinitionBuilder.super.taskGraph(stage, builder);
    }
}
