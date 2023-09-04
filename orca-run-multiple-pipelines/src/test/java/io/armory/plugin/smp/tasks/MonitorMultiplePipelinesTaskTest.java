package io.armory.plugin.smp.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.pipeline.model.PipelineExecutionImpl;
import com.netflix.spinnaker.orca.pipeline.model.PipelineTrigger;
import com.netflix.spinnaker.orca.pipeline.model.StageExecutionImpl;
import com.netflix.spinnaker.orca.pipeline.persistence.ExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType.PIPELINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitorMultiplePipelinesTaskTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PipelineExecutionImpl pipeline;

    private ExecutionRepository executionRepository;

    private final static String testApplication = "test_app";
    private final static String testUser = "test_user";

    @BeforeEach
    public void setup() {
        pipeline = new PipelineExecutionImpl(PIPELINE, testApplication);
        pipeline.setAuthentication(new PipelineExecution.AuthenticationDetails(testUser));

        executionRepository = mock(ExecutionRepository.class);
    }

    @Test
    public void shouldMonitorMultiplePipelinesTaskFinishSuccessfully() {
        var task = new MonitorMultiplePipelinesTask(executionRepository, objectMapper);

        var context = new HashMap<String, Object>();
        context.put("levelNumber", 0);
        context.put("orderOfExecutionsSize", 0);
        context.put("executionIds", List.of());

        var stageExecution = new StageExecutionImpl(pipeline, "runMultiplePipelines", context);

        pipeline.getStages().add(stageExecution);

        var result = task.execute(stageExecution);

        assertEquals(ExecutionStatus.SUCCEEDED, result.getStatus());
    }

    @Test
    public void shouldMonitorMultiplePipelinesTaskFinishSuccessfullyWithRedirect() {
        var testPipelineId1 = "test_child_1";
        var testPipelineId2 = "test_child_2";

        var task = new MonitorMultiplePipelinesTask(executionRepository, objectMapper);

        var context = new HashMap<String, Object>();
        context.put("levelNumber", 0);
        context.put("orderOfExecutions", List.of(List.of(
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId1);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId1);
                }},
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId2);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId2);
                }}
        )));
        context.put("orderOfExecutionsSize", 2);
        context.put("executionIds", List.of(testPipelineId1, testPipelineId2));

        var stageExecution = new StageExecutionImpl(pipeline, "runMultiplePipelines", context);

        pipeline.getStages().add(stageExecution);

        var testParentPipeline = new PipelineExecutionImpl(PIPELINE, testApplication);
        testParentPipeline.setStatus(ExecutionStatus.RUNNING);

        var testPipeline1 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline1.setStatus(ExecutionStatus.SUCCEEDED);
        testPipeline1.setTrigger(new PipelineTrigger(testParentPipeline));

        var testPipeline2 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline2.setStatus(ExecutionStatus.SUCCEEDED);
        testPipeline2.setTrigger(new PipelineTrigger(testParentPipeline));

        when(executionRepository.retrieve(PIPELINE, testPipelineId1)).thenReturn(testPipeline1);
        when(executionRepository.retrieve(PIPELINE, testPipelineId2)).thenReturn(testPipeline2);

        var result = task.execute(stageExecution);

        assertEquals(ExecutionStatus.REDIRECT, result.getStatus());
    }

    @Test
    public void shouldMonitorMultiplePipelinesTaskFinishSuccessfullyWithTerminal() {
        var testPipelineId1 = "test_child_1";
        var testPipelineId2 = "test_child_2";

        var task = new MonitorMultiplePipelinesTask(executionRepository, objectMapper);

        var context = new HashMap<String, Object>();
        context.put("levelNumber", 0);
        context.put("orderOfExecutions", List.of(List.of(
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId1);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId1);
                }},
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId2);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId2);
                }}
        )));
        context.put("orderOfExecutionsSize", 2);
        context.put("executionIds", List.of(testPipelineId1, testPipelineId2));

        var stageExecution = new StageExecutionImpl(pipeline, "runMultiplePipelines", context);

        pipeline.getStages().add(stageExecution);

        var testParentPipeline = new PipelineExecutionImpl(PIPELINE, testApplication);
        testParentPipeline.setStatus(ExecutionStatus.RUNNING);

        var testPipeline1 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline1.setStatus(ExecutionStatus.SUCCEEDED);
        testPipeline1.setTrigger(new PipelineTrigger(testParentPipeline));

        var testPipeline2 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline2.setStatus(ExecutionStatus.TERMINAL);
        testPipeline2.setTrigger(new PipelineTrigger(testParentPipeline));

        when(executionRepository.retrieve(PIPELINE, testPipelineId1)).thenReturn(testPipeline1);
        when(executionRepository.retrieve(PIPELINE, testPipelineId2)).thenReturn(testPipeline2);

        var result = task.execute(stageExecution);

        assertEquals(ExecutionStatus.TERMINAL, result.getStatus());
    }

    @Test
    public void shouldMonitorMultiplePipelinesTaskFinishSuccessfullyWithCanceled() {
        var testPipelineId1 = "test_child_1";
        var testPipelineId2 = "test_child_2";

        var task = new MonitorMultiplePipelinesTask(executionRepository, objectMapper);

        var context = new HashMap<String, Object>();
        context.put("levelNumber", 0);
        context.put("orderOfExecutions", List.of(List.of(
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId1);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId1);
                }},
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId2);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId2);
                }}
        )));
        context.put("orderOfExecutionsSize", 2);
        context.put("executionIds", List.of(testPipelineId1, testPipelineId2));

        var stageExecution = new StageExecutionImpl(pipeline, "runMultiplePipelines", context);

        pipeline.getStages().add(stageExecution);

        var testParentPipeline = new PipelineExecutionImpl(PIPELINE, testApplication);
        testParentPipeline.setStatus(ExecutionStatus.RUNNING);

        var testPipeline1 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline1.setStatus(ExecutionStatus.SUCCEEDED);
        testPipeline1.setTrigger(new PipelineTrigger(testParentPipeline));

        var testPipeline2 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline2.setStatus(ExecutionStatus.CANCELED);
        testPipeline2.setTrigger(new PipelineTrigger(testParentPipeline));

        when(executionRepository.retrieve(PIPELINE, testPipelineId1)).thenReturn(testPipeline1);
        when(executionRepository.retrieve(PIPELINE, testPipelineId2)).thenReturn(testPipeline2);

        var result = task.execute(stageExecution);

        assertEquals(ExecutionStatus.CANCELED, result.getStatus());
    }

    @Test
    public void shouldMonitorMultiplePipelinesTaskFinishSuccessfullyWithRunning() {
        var testPipelineId1 = "test_child_1";
        var testPipelineId2 = "test_child_2";

        var task = new MonitorMultiplePipelinesTask(executionRepository, objectMapper);

        var context = new HashMap<String, Object>();
        context.put("levelNumber", 0);
        context.put("orderOfExecutions", List.of(List.of(
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId1);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId1);
                }},
                new HashMap<String, Object>() {{
                    put("yamlIdentifier", testPipelineId2);
                    put("arguments", new HashMap<>());
                    put("child_pipeline", testPipelineId2);
                }}
        )));
        context.put("orderOfExecutionsSize", 2);
        context.put("executionIds", List.of(testPipelineId1, testPipelineId2));

        var stageExecution = new StageExecutionImpl(pipeline, "runMultiplePipelines", context);

        pipeline.getStages().add(stageExecution);

        var testParentPipeline = new PipelineExecutionImpl(PIPELINE, testApplication);
        testParentPipeline.setStatus(ExecutionStatus.RUNNING);

        var testPipeline1 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline1.setStatus(ExecutionStatus.SUCCEEDED);
        testPipeline1.setTrigger(new PipelineTrigger(testParentPipeline));

        var testPipeline2 = new PipelineExecutionImpl(PIPELINE, testApplication);
        testPipeline2.setStatus(ExecutionStatus.RUNNING);
        testPipeline2.setTrigger(new PipelineTrigger(testParentPipeline));

        when(executionRepository.retrieve(PIPELINE, testPipelineId1)).thenReturn(testPipeline1);
        when(executionRepository.retrieve(PIPELINE, testPipelineId2)).thenReturn(testPipeline2);

        var result = task.execute(stageExecution);

        assertEquals(ExecutionStatus.RUNNING, result.getStatus());
    }
}
