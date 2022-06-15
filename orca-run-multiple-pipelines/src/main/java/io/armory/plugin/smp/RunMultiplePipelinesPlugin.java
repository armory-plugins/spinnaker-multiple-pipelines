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

import com.netflix.spinnaker.kork.plugins.api.spring.SpringLoader;
import com.netflix.spinnaker.kork.plugins.api.spring.SpringLoaderPlugin;
import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import io.armory.plugin.smp.tasks.RunMultiplePipelinesTask;
import javax.annotation.Nonnull;

import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Collections;
import java.util.List;

public class RunMultiplePipelinesPlugin extends SpringLoaderPlugin {
    private static final String ARMORY_IAM_SPRING_LOADER_BEAN_NAME = String.format("Armory.RunMultiplePipelines.%s", SpringLoader.class.getName());
    private static final String SQL_CONFIGURATION_BEAN_NAME = "sqlConfiguration";

    private Logger logger = LoggerFactory.getLogger(RunMultiplePipelinesPlugin.class);
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
        logger.info("Starting RunMultiplePipelines plugin...siuu");
    }

    @Override
    public void stop() {
        logger.info("Stopping RunMultiplePipelines plugin...");
    }

    @Override
    public List<String> getPackagesToScan() {
        return Collections.singletonList("io.armory.plugin.smp");
    }

    @Override
    public void registerBeanDefinitions(BeanDefinitionRegistry registry) {
        super.registerBeanDefinitions(registry);
        if (registry.containsBeanDefinition(SQL_CONFIGURATION_BEAN_NAME)) {
            registry.getBeanDefinition(SQL_CONFIGURATION_BEAN_NAME)
                    .setDependsOn(ARMORY_IAM_SPRING_LOADER_BEAN_NAME);
        }
    }

}

@Extension
class RunMultiplePipelinesStage implements StageDefinitionBuilder {
    @Override
    public void taskGraph(@Nonnull StageExecution stage, @Nonnull TaskNode.Builder builder) {
        builder.withTask("runMultiplePipelines", RunMultiplePipelinesTask.class);
    }
}
