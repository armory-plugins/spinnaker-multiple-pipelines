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
import io.armory.plugin.smp.execution.MyExecutionLauncher;
import io.armory.plugin.smp.tasks.MonitorMultiplePipelinesTask;
import io.armory.plugin.smp.tasks.ParsePipelinesYamlTask;
import io.armory.plugin.smp.tasks.RunMultiplePipelinesTask;
import io.armory.plugin.smp.tasks.SaveOutputsForDetailsTask;
import org.apache.commons.lang3.tuple.Pair;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RunMultiplePipelinesPlugin extends SpringLoaderPlugin {
    private static final String SPRING_LOADER_BEAN_NAME = String.format("Armory.RunMultiplePipelines.%s", SpringLoader.class.getName());
    private static final List<String> ORCA_BEANS_DEPENDING_ON_PLUGIN = List.of(
            "sqlConfiguration",
            "artifactUtils",
            "sqlOrcaQueueConfiguration",
            "dependentPipelineStarter"
    );

    private final Logger logger = LoggerFactory.getLogger(RunMultiplePipelinesPlugin.class);

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
        logger.info("Starting RunMultiplePipelines plugin...");
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
        List<Pair<String, Class>> beanList = Arrays.asList(
                Pair.of("RunMultiplePipelinesStage", RunMultiplePipelinesStage.class),
                Pair.of("RunMultiplePipelinesTask", RunMultiplePipelinesTask.class),
                Pair.of("ParsePipelinesYamlTask", ParsePipelinesYamlTask.class),
                Pair.of("MonitorMultiplePipelinesTask", MonitorMultiplePipelinesTask.class),
                Pair.of("SaveOutputsForDetailsTask", SaveOutputsForDetailsTask.class),
                Pair.of("MyExecutionLauncher", MyExecutionLauncher.class)
        );
        beanList.forEach(curr -> {
            BeanDefinition lazyLoadCredentialsRepositoryDefinition = primaryBeanDefinitionFor(curr.getRight());
            try {
                registry.registerBeanDefinition(curr.getLeft(), lazyLoadCredentialsRepositoryDefinition);
            } catch (BeanDefinitionStoreException e) {
                log.error("Could not register bean {}", lazyLoadCredentialsRepositoryDefinition.getBeanClassName());
                throw new RuntimeException(e);
            }
        });

        super.registerBeanDefinitions(registry);

        ORCA_BEANS_DEPENDING_ON_PLUGIN.forEach(bean -> {
            if (registry.containsBeanDefinition(bean)) {
                registry
                        .getBeanDefinition(bean)
                        .setDependsOn(SPRING_LOADER_BEAN_NAME);
            }
        });
    }
}

@Component
class RunMultiplePipelinesStage implements StageDefinitionBuilder {

    private Logger logger = LoggerFactory.getLogger(RunMultiplePipelinesStage.class);

    @Override
    public void taskGraph(@Nonnull StageExecution stage, @Nonnull TaskNode.Builder builder) {
        builder.withTask("parsePipelinesYamlTask", ParsePipelinesYamlTask.class);
        builder.withLoop(sub -> {
            sub.withTask("runMultiplePipelines", RunMultiplePipelinesTask.class);
            sub.withTask("monitorMultiplePipelinesTask", MonitorMultiplePipelinesTask.class);
        });
        builder.withTask("saveOutputsForDetailsTask", SaveOutputsForDetailsTask.class);
        //TODO: task that handles automatic rollbacks given rollback_onfailure=true
    }

}
