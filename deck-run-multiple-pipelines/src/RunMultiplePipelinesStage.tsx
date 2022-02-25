import { ExecutionDetailsTasks, IStageTypeConfig } from '@spinnaker/core';

import { RunMultiplePipelinesStageExecutionDetails } from './RunMultiplePipelinesStageExecutionDetails';
import { RunMultiplePipelinesStageConfig } from './RunMultiplePipelinesStageConfig';

/*
  Define Spinnaker Stages with IStageTypeConfig.
  Required options: https://github.com/spinnaker/deck/master/app/scripts/modules/core/src/domain/IStageTypeConfig.ts
  - label -> The name of the Stage
  - description -> Long form that describes what the Stage actually does
  - key -> A unique name for the Stage in the UI; ties to Orca backend
  - component -> The rendered React component
  - validateFn -> A validation function for the stage config form.
 */
export const runMultiplePipelinesStage: IStageTypeConfig = {
  key: 'runMultiplePipelines',
  label: `Run Multiple Pipelines`,
  description: 'Stage that triggers pipelines based on yaml file',
  component: RunMultiplePipelinesStageConfig, // stage config
  executionDetailsSections: [RunMultiplePipelinesStageExecutionDetails, ExecutionDetailsTasks],
};
