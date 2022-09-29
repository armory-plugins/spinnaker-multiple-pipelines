package io.armory.plugin.smp.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.LinkedList;
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

        Apps apps = utilityHelper.getApps(context, gson, objectMapper);

        Map<String, Stack<App>> stack_apps = utilityHelper.tryWithStack(apps, objectMapper, gson);

        if(context.isCheckDuplicated() && checkDuplicatedExecution(stack_apps)) {
            stage.appendErrorMessage("Detected two or more duplicated arguments and calling the same child_pipeline. " +
                    "You would have the same execution");
            return TaskResult
                    .builder(ExecutionStatus.TERMINAL)
                    .context(stage.getContext())
                    .build();
        }

        logger.info("Map of apps, detected returning success " + stack_apps.size() + " size");
        stage.getContext().put("stack_apps", stack_apps);

        //remove yamlConfig to reduce pipeline body blob being stored
        stage.getContext().remove("yamlConfig");
        return TaskResult
                .builder(ExecutionStatus.SUCCEEDED)
                .context(stage.getContext())
                .build();
    }

    private boolean checkDuplicatedExecution(Map<String, Stack<App>> stack_apps) throws JsonProcessingException {
        Map<String, Stack<App>> stack_appsCopy = objectMapper.readValue(objectMapper.writeValueAsString(stack_apps), new TypeReference<>() {});
        LinkedList<App> appList = new LinkedList<>();
        for (Stack<App> stack : stack_appsCopy.values()) {
            appList.add(stack.pop());
        }
        if ( appList.size() != appList.stream().distinct().count()) {
            logger.warn("Detected " + (appList.size()+1 - appList.stream().distinct().count())  + " duplicated arguments and calling the same child_pipeline");
            return true;
        }
        return false;
    }

}
