package io.armory.plugin.smp.tasks

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.orca.api.pipeline.OverridableTimeoutRetryableTask;
import com.netflix.spinnaker.orca.api.pipeline.TaskResult;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import com.netflix.spinnaker.orca.front50.pipeline.MonitorPipelineStage;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit
import java.util.stream.Collectors;

import static com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType.PIPELINE;

/**
 * This is a copy of <a href="https://github.com/spinnaker/orca/blob/b9077fb8187f6fb9612681b528bd760ef50e4534/orca-front50/src/main/groovy/com/netflix/spinnaker/orca/front50/tasks/MonitorPipelineTask.groovy">MonitorPipelineTask</a>
 * <p> changes -> pass this condition (stage.type == MonitorPipelineStage.PIPELINE_CONFIG_TYPE) </p>
 * <p> get and update PipelinesExecutions list to refer to it in outputs </p>
 */
@Component
class MonitorMultiplePipelinesTask implements OverridableTimeoutRetryableTask {

    ExecutionRepository executionRepository
    ObjectMapper objectMapper

    long backoffPeriod = TimeUnit.SECONDS.toMillis(15)
    long timeout = TimeUnit.HOURS.toMillis(24)

    private final Logger logger = LoggerFactory.getLogger(MonitorMultiplePipelinesTask.class)

    @Autowired
    public MonitorMultiplePipelinesTask(ExecutionRepository executionRepository, ObjectMapper objectMapper) {
        this.executionRepository = executionRepository
        this.objectMapper = objectMapper
    }

    @Override
    TaskResult execute(StageExecution stage) {
        stage.getContext().put("monitorBehavior", MonitorPipelineStage.MonitorBehavior.WaitForAllToComplete)
        List<String> pipelineIds
        boolean isLegacyStage = false
        MonitorPipelineStage.StageParameters stageData = stage.mapTo(MonitorPipelineStage.StageParameters.class)

        if (stage.type == "runMultiplePipelines") {
            pipelineIds = stageData.executionIds
        } else {
            pipelineIds = Collections.singletonList(stageData.executionId)
            isLegacyStage = true
        }

        HashMap<String, MonitorPipelineStage.ChildPipelineStatusDetails> pipelineStatuses = new HashMap<>(pipelineIds.size())
        List<PipelineExecution> pipelineExecutionsOutput = new ArrayList<>()
        PipelineExecution firstPipeline

        for (String pipelineId : pipelineIds) {
            PipelineExecution childPipeline = executionRepository.retrieve(PIPELINE, pipelineId)
            pipelineExecutionsOutput.add(childPipeline)
            if (firstPipeline == null) {
                // Capture the first pipeline, since if there is only one, we will return its context as part of TaskResult
                firstPipeline = childPipeline
            }
            MonitorPipelineStage.ChildPipelineStatusDetails details = new MonitorPipelineStage.ChildPipelineStatusDetails()
            details.status = childPipeline.status
            details.application = childPipeline.application
            pipelineStatuses.put(pipelineId, details)

            if (childPipeline.status.halt) {
                details.exception = new MonitorPipelineStage.ChildPipelineException()

                // indicates a failure of some sort
                def terminalStages = childPipeline.stages.findAll { s -> s.status == ExecutionStatus.TERMINAL }
                List<String> errors = terminalStages
                        .findResults { s ->
                            if (s.context["exception"]?.details) {
                                return [(s.context["exception"].details.errors ?: s.context["exception"].details.error)]
                                        .flatten()
                                        .collect { e -> buildExceptionMessage(childPipeline.name, e as String, s) }
                            }
                            if (s.context["kato.tasks"]) {
                                return s.context["kato.tasks"]
                                        .findAll { k -> k.status?.failed }
                                        .findResults { k ->
                                            String message = k.exception?.message ?: k.history ? ((List<String>) k.history).last() : null
                                            return message ? buildExceptionMessage(childPipeline.name, message, s) : null
                                        }
                            }
                        }
                        .flatten()

                details.exception.details.errors = errors

                def haltingStage = terminalStages.find { it.status.halt }
                if (haltingStage) {
                    details.exception.source.executionId = childPipeline.id
                    details.exception.source.stageId = haltingStage.id
                    details.exception.source.stageName = haltingStage.name
                    details.exception.source.stageIndex = childPipeline.stages.indexOf(haltingStage)
                }
            }
        }

        boolean allPipelinesSucceeded = pipelineStatuses.every { it.value.status == ExecutionStatus.SUCCEEDED }
        boolean allPipelinesCompleted = pipelineStatuses.every { it.value.status.complete }
        boolean anyPipelinesFailed = pipelineStatuses.any { effectiveStatus(it.value.status) == ExecutionStatus.TERMINAL }

        MonitorPipelineStage.StageResult result = new MonitorPipelineStage.StageResult()
        Map<String, Object> context = new HashMap<>()

        if (isLegacyStage) {
            context.put("status", firstPipeline.status)
            if (anyPipelinesFailed) {
                context.put("exception", objectMapper.convertValue(pipelineStatuses.values().first().exception, Map))
            }
        } else {
            result.executionStatuses = pipelineStatuses
        }
        List<PipelineExecution> pipelineExecutions = pipelineExecutionsOutput

        if (pipelineIds.size() == 1) {
            result.insertPipelineContext(firstPipeline.getContext())
        }

        if (allPipelinesSucceeded) {
            logger.info("All child pipelines SUCCEEDED")
            pipelineExecutions = filterStages(pipelineExecutions)
            stage.outputs.put("executionsList", pipelineExecutions)
            return buildTaskResult(ExecutionStatus.SUCCEEDED, context, result)
        }

        if (anyPipelinesFailed) {
            if (allPipelinesCompleted || stageData.monitorBehavior == MonitorPipelineStage.MonitorBehavior.FailFast) {
                logger.info("Some child pipelines FAILED")
                stage.appendErrorMessage("At least one monitored pipeline failed, look for errors in failed pipelines")
                pipelineExecutions = filterStages(pipelineExecutions)
                stage.outputs.put("executionsList", pipelineExecutions)
                return buildTaskResult(ExecutionStatus.TERMINAL, context, result)
            }
        }

        // Finally, if all pipelines completed and we didn't catch that case above it means at least one of those pipelines was CANCELED
        // and we should propagate that result up
        if (allPipelinesCompleted) {
            logger.info("Some child pipelines were CANCELED")
            stage.appendErrorMessage("At least one monitored pipeline was cancelled")
            pipelineExecutions = filterStages(pipelineExecutions)
            stage.outputs.put("executionsList", pipelineExecutions)
            return buildTaskResult(ExecutionStatus.CANCELED, context, result)
        }

        return buildTaskResult(ExecutionStatus.RUNNING, context, result)
    }

    private List<PipelineExecution> filterStages(List<PipelineExecution> executions) {
        ArrayList<PipelineExecution> filteredStagesExecutions = new ArrayList<>()
        for (PipelineExecution pipelineExecution : executions) {
             List<StageExecution> deployStages = pipelineExecution.getStages().stream()
                     .filter({ stage -> stage.name.startsWith("Deploy") })
                     .filter({ stage -> stage.status != ExecutionStatus.SKIPPED })
                     .collect(Collectors.toList())
            deployStages = deployStages.stream().filter({ stage ->
                List<Map<String, Object>> manifests = objectMapper.readValue(objectMapper.writeValueAsString(stage.getOutputs().get("manifests")), new TypeReference<List<Map<String, Object>>>() {})
                if (manifests.get(0).get("kind").equals("DaemonSet") ||
                        manifests.get(0).get("kind").equals("Deployment") ||
                        manifests.get(0).get("kind").equals("StatefulSet")) {
                    return true
                }
                return false
            }).collect(Collectors.toList())
            pipelineExecution.getStages().clear()
            pipelineExecution.getStages().addAll(deployStages)
            filteredStagesExecutions.add(pipelineExecution)
        }
        return filteredStagesExecutions
    }

    private buildTaskResult(ExecutionStatus status, Map<String, Object> context, MonitorPipelineStage.StageResult result) {
        return TaskResult.builder(status)
                .context(context)
                .outputs(objectMapper.convertValue(result, Map))
                .build()
    }

    private static effectiveStatus(ExecutionStatus status) {
        if (!status.halt) {
            return status
        }

        if (status == ExecutionStatus.CANCELED) {
            return ExecutionStatus.CANCELED
        }

        return ExecutionStatus.TERMINAL
    }

    private static String buildExceptionMessage(String pipelineName, String message, StageExecution stage) {
        "Exception in child pipeline stage (${pipelineName}: ${stage.name ?: stage.type}): ${message}"
    }
}
