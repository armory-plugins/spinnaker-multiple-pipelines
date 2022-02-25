import React from 'react';
import { get, isEmpty } from 'lodash';

import type { IFormikStageConfigInjectedProps } from '@spinnaker/core';

import { YamlEditor, yamlDocumentsToString } from '@spinnaker/core';

// import { YamlEditor } from './yamlEditor/YamlEditor';

interface IInputYamlForm {
  inputYaml: string;
}

export class InputYamlForm extends React.Component<IFormikStageConfigInjectedProps, IInputYamlForm> {
  constructor(props: IFormikStageConfigInjectedProps) {
    super(props);
    const YAML: any[] = get(props.formik.values, 'YAML');
    this.state = {
      inputYaml: !isEmpty(YAML) ? yamlDocumentsToString(YAML) : '',
    };
  }

  private handleRawManifestChange = (inputYaml: string, YAML: any): void => {
    this.setState({ inputYaml });
    this.props.formik.setFieldValue('YAML', YAML);
  };

  public render() {
    return (
      <div className="form-horizontal">
        <h4>Yaml Configuration</h4>
        <YamlEditor value={this.state.inputYaml} onChange={this.handleRawManifestChange} />
      </div>
    );
  }
}
