import React from 'react';
import { FormikStageConfig, IStageConfigProps } from '@spinnaker/core';

import { InputYamlForm } from './InputYamlForm';

/*
  IStageConfigProps defines properties passed to all Spinnaker Stages.
  See IStageConfigProps.ts (https://github.com/spinnaker/deck/blob/master/app/scripts/modules/core/src/pipeline/config/stages/common/IStageConfigProps.ts) for a complete list of properties.
  This method returns JSX (https://reactjs.org/docs/introducing-jsx.html) that gets displayed in the Spinnaker UI.
 */
export function RunMultiplePipelinesStageConfig(props: IStageConfigProps) {
  return (
    <div className="RunMultiplePipelinesStageConfig">
      <FormikStageConfig {...props} onChange={props.updateStage} render={(props) => <InputYamlForm {...props} />} />
    </div>
  );
}
