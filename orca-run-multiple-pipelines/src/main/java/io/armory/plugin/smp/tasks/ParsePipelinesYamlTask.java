package io.armory.plugin.smp.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.netflix.spinnaker.orca.api.pipeline.Task;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import io.armory.plugin.smp.config.RunMultiplePipelinesContext;
import io.armory.plugin.smp.config.UtilityHelper;
import io.armory.plugin.smp.parseyml.App;
import io.armory.plugin.smp.parseyml.Apps;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Stack;

@Component
public class ParsePipelinesYamlTask implements Task {

    private final Logger logger = LoggerFactory.getLogger(ParsePipelinesYamlTask.class);
    private final ObjectMapper objectMapper;

    public ParsePipelinesYamlTask(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        RunMultiplePipelinesContext context = stage.mapTo(RunMultiplePipelinesContext.class);
        Gson gson = new Gson();
        UtilityHelper utilityHelper = new UtilityHelper();

        //TODO: evaluate RunMultiplePipelinesContext.yamlConfig return TERMINAL and appropriate error message

        Apps apps = utilityHelper.getApps(context, gson, objectMapper);

        Map<String, Stack<App>> stack_apps = utilityHelper.tryWithStack(apps, objectMapper, gson);
        logger.info("Map of apps, detected returning success " + stack_apps.size() + " size");
        stage.getContext().put("apps", apps);
        stage.getContext().put("stack_apps", stack_apps);

        return TaskResult
                .builder(ExecutionStatus.SUCCEEDED)
                .context(stage.getContext())
                .build();
    }

}