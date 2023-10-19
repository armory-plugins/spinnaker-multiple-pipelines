import React from 'react';
import { render, screen } from '@testing-library/react';
import { Application, IPipeline, IStage, SpinFormik } from '@spinnaker/core';

import { InputYamlForm } from './InputYamlForm';

describe('InputYamlForm', () => {
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

    render(
      <SpinFormik<IStage>
        initialValues={{
          name: 'test_stage',
          refId: 1,
          requisiteStageRefIds: [],
          type: 'test',
          yamlConfig: ['foo: 101', 'bar: 102'],
        }}
        onSubmit={() => {}}
        render={(formik) => (
          <InputYamlForm application={application} pipeline={pipeline} formik={formik}/>
        )}
      />
    );

    const title = screen.getByText('Yaml Configuration');
    expect(title).toBeInTheDocument();
  });
});
