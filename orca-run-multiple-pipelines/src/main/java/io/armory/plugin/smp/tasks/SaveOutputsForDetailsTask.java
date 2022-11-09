package io.armory.plugin.smp.tasks;

import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import io.armory.plugin.smp.config.RunMultiplePipelinesOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;

@Component
public class SaveOutputsForDetailsTask implements Task {

    private final Logger logger = LoggerFactory.getLogger(SaveOutputsForDetailsTask.class);

    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        logger.info("started SaveOutputsTask task loop ended");
        stage.getOutputs().clear();
        stage.getContext().remove("orderOfExecutions");
        List<RunMultiplePipelinesOutputs> multiplePipelinesOutputsList = (List<RunMultiplePipelinesOutputs>) stage.getContext().remove("runMultiplePipelinesOutputs");
        stage.getOutputs().put("executionsList", multiplePipelinesOutputsList);

        return TaskResult
                .builder(ExecutionStatus.SUCCEEDED)
                .context(stage.getContext())
                .outputs(stage.getOutputs())
                .build();
    }
}
