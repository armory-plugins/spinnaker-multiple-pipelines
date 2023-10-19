import { ExecutionDetailsTasks } from '@spinnaker/core';

import { runMultiplePipelinesStage } from "./RunMultiplePipelinesStage";
import { RunMultiplePipelinesStageConfig } from './RunMultiplePipelinesStageConfig';
import { RunMultiplePipelinesStageExecutionDetails } from './RunMultiplePipelinesStageExecutionDetails';

describe('runMultiplePipelinesStage', () => {
  it('should be resolved correctly', async () => {
    expect(runMultiplePipelinesStage.key).toBe('runMultiplePipelines');
    expect(runMultiplePipelinesStage.label).toBe('Run Multiple Pipelines');
    expect(runMultiplePipelinesStage.description).toBe('Stage that triggers pipelines based on yaml file');
    expect(runMultiplePipelinesStage.component).toBe(RunMultiplePipelinesStageConfig);

    expect(runMultiplePipelinesStage.executionDetailsSections[0]).toBe(RunMultiplePipelinesStageExecutionDetails);
    expect(runMultiplePipelinesStage.executionDetailsSections[1]).toBe(ExecutionDetailsTasks);
  });
});
