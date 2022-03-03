import React from 'react';

import { ExecutionDetailsSection, IExecutionDetailsSectionProps } from '@spinnaker/core';

/*
 * You can use this component to provide information to users about
 * how the stage was configured and the results of its execution.
 *
 * In general, you will access two properties of `props.stage`:
 * - `props.stage.outputs` maps to your SimpleStage's `Output` class.
 * - `props.stage.context` maps to your SimpleStage's `Context` class.
 */
export class RunMultiplePipelinesStageExecutionDetails extends React.Component<IExecutionDetailsSectionProps> {
  public static title = 'runMultiplePipelines';
  public render() {
    return (
      <ExecutionDetailsSection name={this.props.name} current={this.props.current}>
        <div>
          <p>Message: {this.props.stage.outputs.my_message}</p>
        </div>
      </ExecutionDetailsSection>
    );
  }
}
