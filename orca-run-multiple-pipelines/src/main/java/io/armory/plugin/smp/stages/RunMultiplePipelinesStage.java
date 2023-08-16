package io.armory.plugin.smp.stages;

import com.netflix.spinnaker.kork.plugins.api.spring.ExposeToApp;
import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import io.armory.plugin.smp.tasks.MonitorMultiplePipelinesTask;
import io.armory.plugin.smp.tasks.ParsePipelinesYamlTask;
import io.armory.plugin.smp.tasks.RunMultiplePipelinesTask;
import io.armory.plugin.smp.tasks.SaveOutputsForDetailsTask;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@ExposeToApp
@Component
public class RunMultiplePipelinesStage implements StageDefinitionBuilder {

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
