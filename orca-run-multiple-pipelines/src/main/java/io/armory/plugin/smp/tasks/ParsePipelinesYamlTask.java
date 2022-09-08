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

    static Apps APPS;
    static Map<String, Stack<App>> STACK_APPS;

    @SneakyThrows
    @Nonnull
    @Override
    public TaskResult execute(@Nonnull StageExecution stage) {
        RunMultiplePipelinesContext context = stage.mapTo(RunMultiplePipelinesContext.class);
        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper();
        UtilityHelper utilityHelper = new UtilityHelper();

        //TODO: evaluate RunMultiplePipelinesContext.yamlConfig return TERMINAL and appropriate error message

        APPS = utilityHelper.getApps(context, gson, mapper);

        STACK_APPS = utilityHelper.tryWithStack(APPS, mapper, gson);
        logger.info("Map of apps, detected returning success " + STACK_APPS.size() + " size");

        return TaskResult
                .builder(ExecutionStatus.SUCCEEDED)
                .build();
    }

    public static Apps getApps() {
        return APPS;
    }

    public static Map<String, Stack<App>> getStackApps() {
        return STACK_APPS;
    }
}