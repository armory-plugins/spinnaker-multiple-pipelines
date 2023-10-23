import React from 'react';
import { render, screen } from '@testing-library/react';
import { Application, IPipeline, IStage } from '@spinnaker/core';

import { RunMultiplePipelinesStageConfig } from './RunMultiplePipelinesStageConfig';

describe('RunMultiplePipelinesStageConfig', () => {
  it('should render component correctly', async () => {
    const application = new Application('test_application', null, []);

    const pipeline: IPipeline = {
      id: 'test_pipeline',
      name: 'test_pipeline',
      stages: [],
      triggers: [],
      application: 'test_application',
      limitConcurrent: true,
      keepWaitingPipelines: false,
      spelEvaluator: 'v4',
      parameterConfig: [],
    };

    const stage: IStage = {
      name: 'test_stage',
      refId: 1,
      requisiteStageRefIds: [],
      type: 'test',
      yamlConfig: ['foo: 101', 'bar: 102'],
    };

    render(
      <RunMultiplePipelinesStageConfig
        application={application}
        pipeline={pipeline}
        stage={stage}
        stageFieldUpdated={() => {}}
        updateStage={() => {}}
        updateStageField={() => {}}
      />
    );

    const title = screen.getByText('Yaml Configuration');
    expect(title).toBeInTheDocument();
  });
});
