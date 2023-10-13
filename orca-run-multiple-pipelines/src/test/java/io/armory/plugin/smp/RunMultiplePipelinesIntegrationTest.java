package io.armory.plugin.smp;

import io.armory.plugin.smp.stages.RunMultiplePipelinesStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunMultiplePipelinesIntegrationTest extends RunMultiplePipelinesFixture {

    @Test
    public void shouldResolveRunMultiplePipelinesStageCorrectly() {
        var stageDefinitionBuilder = this.stageResolver.getStageDefinitionBuilder(
                RunMultiplePipelinesStage.class.getTypeName(), "runMultiplePipelines");

        assertEquals("runMultiplePipelines", stageDefinitionBuilder.getType());
    }
}
