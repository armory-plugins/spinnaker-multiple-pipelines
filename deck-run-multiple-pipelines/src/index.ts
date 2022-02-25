import { IDeckPlugin } from '@spinnaker/core';
import { runMultiplePipelinesStage } from './RunMultiplePipelinesStage';

export const plugin: IDeckPlugin = {
  stages: [runMultiplePipelinesStage],
};
