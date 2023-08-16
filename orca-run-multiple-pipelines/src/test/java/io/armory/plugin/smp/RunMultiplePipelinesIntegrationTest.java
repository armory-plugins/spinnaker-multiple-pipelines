package io.armory.plugin.smp;

import com.netflix.spinnaker.orca.api.pipeline.graph.StageDefinitionBuilder;
import io.armory.plugin.smp.stages.RunMultiplePipelinesStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunMultiplePipelinesIntegrationTest extends RunMultiplePipelinesFixture {

    @Test
    public void resolveToCorrectTypeTest() {
        System.out.println(this.stageResolver);
        StageDefinitionBuilder stageDefinitionBuilder = this.stageResolver.getStageDefinitionBuilder(
                RunMultiplePipelinesStage.class.getTypeName(), "runMultiplePipelines");

        assertEquals(stageDefinitionBuilder.getType(), "runMultiplePipelines" , "Expected stageDefinitionBuilder to be of type runMultiplePipelines");
    }
}
