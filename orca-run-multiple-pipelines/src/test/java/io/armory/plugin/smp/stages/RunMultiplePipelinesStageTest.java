package io.armory.plugin.smp.stages;

import com.netflix.spinnaker.orca.api.pipeline.graph.TaskNode;
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution;
import io.armory.plugin.smp.tasks.MonitorMultiplePipelinesTask;
import io.armory.plugin.smp.tasks.ParsePipelinesYamlTask;
import io.armory.plugin.smp.tasks.RunMultiplePipelinesTask;
import io.armory.plugin.smp.tasks.SaveOutputsForDetailsTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class RunMultiplePipelinesStageTest {

    @Mock
    private StageExecution stage;
    private RunMultiplePipelinesStage runMultiplePipelinesStage;

    @BeforeEach
    void setup() {
        runMultiplePipelinesStage = new RunMultiplePipelinesStage();
    }

    @Test
    public void shouldBuildTaskGraphCorrectly() {
        var taskGraph = runMultiplePipelinesStage.buildTaskGraph(stage);
        var taskGraphIterator = taskGraph.iterator();

        var taskDefinition = (TaskNode.TaskDefinition)taskGraphIterator.next();

        assertEquals("parsePipelinesYamlTask", taskDefinition.getName());
        assertEquals(ParsePipelinesYamlTask.class, taskDefinition.getImplementingClass());

        var subTaskGraph = (TaskNode.TaskGraph)taskGraphIterator.next();
        var subTaskGraphIterator = subTaskGraph.iterator();

        var subTaskDefinition = (TaskNode.TaskDefinition)subTaskGraphIterator.next();
        assertEquals("runMultiplePipelines", subTaskDefinition.getName());
        assertEquals(RunMultiplePipelinesTask.class, subTaskDefinition.getImplementingClass());

        subTaskDefinition = (TaskNode.TaskDefinition)subTaskGraphIterator.next();
        assertEquals("monitorMultiplePipelinesTask", subTaskDefinition.getName());
        assertEquals(MonitorMultiplePipelinesTask.class, subTaskDefinition.getImplementingClass());

        taskDefinition = (TaskNode.TaskDefinition)taskGraphIterator.next();

        assertEquals("saveOutputsForDetailsTask", taskDefinition.getName());
        assertEquals(SaveOutputsForDetailsTask.class, taskDefinition.getImplementingClass());
    }
}
