import React from 'react';
import { UIRouter, pushStateLocationPlugin } from '@uirouter/react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  Application,
  IStageTypeConfig,
  IExecutionDetailsSection,
  IExecution,
  IExecutionStage,
  PipelineConfigService
} from '@spinnaker/core';

jest.mock('@spinnaker/core', () => ({
  ...jest.requireActual('@spinnaker/core'),
  PipelineConfigService: {
    savePipeline: jest.fn(),
    triggerPipeline: jest.fn(),
  },
}));

import { RunMultiplePipelinesStageExecutionDetails } from './RunMultiplePipelinesStageExecutionDetails';

describe('RunMultiplePipelinesStageExecutionDetails', () => {
  beforeAll(() => {
    window.spinnaker.application = {
      runningExecutions: {
        data: []
      },
      pipelineConfigs: {
        data: [],
      },
    };
    window.spinnaker.executionService = {
      cancelExecution: jest.fn(),
    };
  })

  afterEach(() => {
    window.spinnaker.application = {
      runningExecutions: {
        data: []
      },
      pipelineConfigs: {
        data: [],
      },
    };
    window.spinnaker.executionService = {
      cancelExecution: jest.fn(),
    };
  });

  it('should not be displayed execution details for a stage when that stage is not the current one', async () => {
    const application = new Application('test_application', null, []);

    const config: IStageTypeConfig = {
      key: 'test_stage_type'
    };

    const execution: IExecution = {
      id: 'test_execution',
      application: 'test_application',
      authentication: { user: 'test_user' },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const stage: IExecutionStage = {
      id: "test_execution_stage",
      name: "test_execution_stage",
      context: {},
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: "",
      refId: undefined,
      requisiteStageRefIds: undefined,
      runningTime: "",
      runningTimeInMs: 1000 * 60,
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      tasks: [],
      type: "",
      outputs: {}
    };

    const detailsSections: IExecutionDetailsSection[] = [];

    const { container } = render(
      <RunMultiplePipelinesStageExecutionDetails
        name="test_stage_execution_a"
        current="test_stage_execution_b"
        config={config}
        application={application}
        execution={execution}
        stage={stage}
        detailsSections={detailsSections}
        provider="test_provider"
      />
    );

    const appLabel = screen.queryByText('App');
    expect(appLabel).not.toBeInTheDocument();

    const startedLabel = screen.queryByText('Started');
    expect(startedLabel).not.toBeInTheDocument();

    const durationLabel = screen.queryByText('Duration');
    expect(durationLabel).not.toBeInTheDocument();

    const statusLabel = screen.queryByText('Status');
    expect(statusLabel).not.toBeInTheDocument();

    const actionLabel = screen.queryByText('Action');
    expect(actionLabel).not.toBeInTheDocument();

    const cancelAllExecutionsBtn = screen.queryByText('Cancel all executions');
    expect(cancelAllExecutionsBtn).not.toBeInTheDocument();
  });

  it('should be displayed execution details for a stage when that stage is the current one', async () => {
    const testApplication = new Application('test_application', null, []);

    const testParentExecution: IExecution = {
      id: 'test_execution_parent',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const testChildExecution: IExecution = {
      id: 'test_execution_child',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const stage: IExecutionStage = {
      id: "test_stage",
      name: "test_stage",
      context: {},
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: "",
      refId: undefined,
      requisiteStageRefIds: undefined,
      runningTime: "",
      runningTimeInMs: 1000 * 60,
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      tasks: [],
      type: "",
      outputs: {}
    };

    const detailsSections: IExecutionDetailsSection[] = [];

    window.spinnaker.application = {
      runningExecutions: {
        data: [
          {
            name: testChildExecution.name,
            application: testApplication,
            trigger: {
              correlationId: `foo_${stage.id}_bar`,
              parentExecution: {
                trigger: {
                  executionIdentifier: testParentExecution.id
                },
              },
            },
            status: 'RUNNING',
            runningTimeInMs: testChildExecution.runningTimeInMs,
            startTime: testChildExecution.startTime,
          },
        ],
      },
    };

    render(
      <UIRouter plugins={[pushStateLocationPlugin]} states={[]}>
        <RunMultiplePipelinesStageExecutionDetails
          name="test_stage_execution"
          current="test_stage_execution"
          config={{
            key: 'test_type'
          }}
          application={testApplication}
          execution={testChildExecution}
          stage={stage}
          detailsSections={detailsSections}
          provider="test_provider"
        />
      </UIRouter>
    );

    const appLabel = screen.getByText('App');
    expect(appLabel).toBeInTheDocument();

    const startedLabel = screen.getByText('Started');
    expect(startedLabel).toBeInTheDocument();

    const durationLabel = screen.getByText('Duration');
    expect(durationLabel).toBeInTheDocument();

    const statusLabel = screen.getByText('Status');
    expect(statusLabel).toBeInTheDocument();

    const actionLabel = screen.getByText('Action');
    expect(actionLabel).toBeInTheDocument();

    const cancelAllExecutionsBtn = screen.getByText('Cancel all executions');
    expect(cancelAllExecutionsBtn).toBeInTheDocument();

    const runningTimeInMs = screen.getByText('01:00');
    expect(runningTimeInMs).toBeInTheDocument();

    const status = screen.getByText('RUNNING');
    expect(status).toBeInTheDocument();
  });

  it('should cancel execution', async () => {
    window.spinnaker.executionService.cancelExecution = jest.fn().mockReturnValue(Promise.resolve());

    const testApplication = new Application('test_application', null, []);

    const testParentExecution: IExecution = {
      id: 'test_execution_parent',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const testChildExecution: IExecution = {
      id: 'test_execution_child',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const stage: IExecutionStage = {
      id: "test_stage",
      name: "test_stage",
      context: {},
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: "",
      refId: undefined,
      requisiteStageRefIds: undefined,
      runningTime: "",
      runningTimeInMs: 1000 * 60,
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      tasks: [],
      type: "",
      outputs: {}
    };

    const detailsSections: IExecutionDetailsSection[] = [];

    window.spinnaker.application = {
      runningExecutions: {
        data: [
          {
            name: testChildExecution.name,
            application: testApplication,
            trigger: {
              correlationId: `foo_${stage.id}_bar`,
              parentExecution: {
                trigger: {
                  executionIdentifier: testParentExecution.id
                },
              },
            },
            status: 'RUNNING',
            runningTimeInMs: testChildExecution.runningTimeInMs,
            startTime: testChildExecution.startTime,
          },
        ],
      },
    };

    render(
      <UIRouter plugins={[pushStateLocationPlugin]} states={[]}>
        <RunMultiplePipelinesStageExecutionDetails
          name="test_stage_execution"
          current="test_stage_execution"
          config={{
            key: 'test_type'
          }}
          application={testApplication}
          execution={testChildExecution}
          stage={stage}
          detailsSections={detailsSections}
          provider="test_provider"
        />
      </UIRouter>
    );

    const cancelBtn = screen.getByTestId('cancel-btn');

    await userEvent.click(cancelBtn);

    await waitFor(() => {
      const titleLabel = screen.getByText(`Really stop pipeline execution of ${testParentExecution.id}?`);
      expect(titleLabel).toBeInTheDocument();
    });

    const stopRunningPipelineLabel = screen.getByText('Stop running pipeline');
    expect(stopRunningPipelineLabel).toBeInTheDocument();

    await userEvent.click(stopRunningPipelineLabel);

    expect(window.spinnaker.executionService.cancelExecution).toBeCalled();
  });

  it('should cancel all executions', async () => {
    window.spinnaker.executionService.cancelExecution = jest.fn().mockReturnValue(Promise.resolve());

    const testApplication = new Application('test_application', null, []);

    const testParentExecution: IExecution = {
      id: 'test_execution_parent',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const testChildExecution: IExecution = {
      id: 'test_execution_child',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const stage: IExecutionStage = {
      id: "test_stage",
      name: "test_stage",
      context: {},
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: "",
      refId: undefined,
      requisiteStageRefIds: undefined,
      runningTime: "",
      runningTimeInMs: 1000 * 60,
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      tasks: [],
      type: "",
      outputs: {}
    };

    const detailsSections: IExecutionDetailsSection[] = [];

    window.spinnaker.application = {
      runningExecutions: {
        data: [
          {
            name: testChildExecution.name,
            application: testApplication,
            trigger: {
              correlationId: `foo_${stage.id}_bar`,
              parentExecution: {
                trigger: {
                  executionIdentifier: testParentExecution.id
                },
              },
            },
            status: 'RUNNING',
            runningTimeInMs: testChildExecution.runningTimeInMs,
            startTime: testChildExecution.startTime,
          },
        ],
      },
    };

    render(
      <UIRouter plugins={[pushStateLocationPlugin]} states={[]}>
        <RunMultiplePipelinesStageExecutionDetails
          name="test_stage_execution"
          current="test_stage_execution"
          config={{
            key: 'test_type'
          }}
          application={testApplication}
          execution={testChildExecution}
          stage={stage}
          detailsSections={detailsSections}
          provider="test_provider"
        />
      </UIRouter>
    );

    const cancelAllLabel = screen.getByText('Cancel all executions');

    await userEvent.click(cancelAllLabel);

    await waitFor(() => {
      const titleLabel = screen.getByText('Really stop execution of all apps?');
      expect(titleLabel).toBeInTheDocument();
    });

    const stopAllPipelinesLabel = screen.getByText('Stop all pipelines');
    expect(stopAllPipelinesLabel).toBeInTheDocument();

    await userEvent.click(stopAllPipelinesLabel);

    expect(window.spinnaker.executionService.cancelExecution).toBeCalled();
  });

  it('should rollback execution', async () => {
    window.spinnaker.application.pipelineConfigs.data = [
      {
        name: 'rollbackOnFailure',
      },
    ];

    jest.spyOn(PipelineConfigService, 'savePipeline').mockReturnValue(Promise.resolve());
    jest.spyOn(PipelineConfigService, 'triggerPipeline').mockReturnValue(Promise.resolve('test_pipeline'));

    const testApplication = new Application('test_application', null, []);

    const testParentExecution: IExecution = {
      id: 'test_execution_parent',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const testChildExecution: IExecution = {
      id: 'test_execution_child',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const stage: IExecutionStage = {
      id: "test_stage",
      name: "test_stage",
      context: {},
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: "",
      refId: undefined,
      requisiteStageRefIds: undefined,
      runningTime: "",
      runningTimeInMs: 1000 * 60,
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      tasks: [],
      type: "",
      outputs: {
        executionsList: [
          {
            id: testChildExecution.id,
            executionIdentifier: testParentExecution.id,
            startTime: testChildExecution.startTime,
            endTime: testChildExecution.startTime + 1000 * 60,
            status: 'SUCCEEDED',
            artifactCreated: {},
          },
        ],
      },
    };

    const detailsSections: IExecutionDetailsSection[] = [];

    render(
      <UIRouter plugins={[pushStateLocationPlugin]} states={[]}>
        <RunMultiplePipelinesStageExecutionDetails
          name="test_stage_execution"
          current="test_stage_execution"
          config={{
            key: 'test_type'
          }}
          application={testApplication}
          execution={testChildExecution}
          stage={stage}
          detailsSections={detailsSections}
          provider="test_provider"
        />
      </UIRouter>
    );

    const appLabel = screen.getByText('App');
    expect(appLabel).toBeInTheDocument();

    const startedLabel = screen.getByText('Started');
    expect(startedLabel).toBeInTheDocument();

    const durationLabel = screen.getByText('Duration');
    expect(durationLabel).toBeInTheDocument();

    const statusLabel = screen.getByText('Status');
    expect(statusLabel).toBeInTheDocument();

    const actionLabel = screen.getByText('Action');
    expect(actionLabel).toBeInTheDocument();

    const runningTimeInMs = screen.getByText('01:00');
    expect(runningTimeInMs).toBeInTheDocument();

    const status = screen.getByText('SUCCEEDED');
    expect(status).toBeInTheDocument();

    const rollbackBtn = screen.getByTestId('rollback-btn');

    await userEvent.click(rollbackBtn);

    await waitFor(() => {
      const titleLabel = screen.getByText(`Really perform Rollback of ${testParentExecution.id}?`);
      expect(titleLabel).toBeInTheDocument();
    });

    const rollbackLabel = screen.getByText('Rollback');

    await userEvent.click(rollbackLabel);

    expect(PipelineConfigService.savePipeline).toBeCalled();
    expect(PipelineConfigService.triggerPipeline).toBeCalled();
  });

  it('should rollback all executions', async () => {
    window.spinnaker.application.pipelineConfigs.data = [
      {
        name: 'rollbackOnFailure',
      },
    ];

    jest.spyOn(PipelineConfigService, 'savePipeline').mockReturnValue(Promise.resolve());
    jest.spyOn(PipelineConfigService, 'triggerPipeline').mockReturnValue(Promise.resolve('test_pipeline'));

    const testApplication = new Application('test_application', null, []);

    const testParentExecution: IExecution = {
      id: 'test_execution_parent',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const testChildExecution: IExecution = {
      id: 'test_execution_child',
      application: testApplication.name,
      authentication: {
        user: 'test_user'
      },
      deploymentTargets: [],
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: '',
      runningTime: '',
      runningTimeInMs: 1000 * 60,
      stages: [],
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      trigger: {
        user: 'test_user',
        type: 'test_trigger',
        enabled: true
      },
      user: 'test_user'
    };

    const stage: IExecutionStage = {
      id: "test_stage",
      name: "test_stage",
      context: {},
      endTime: 0,
      failureMessage: "",
      getValueFor(k: string): any {
      },
      hasNotStarted: false,
      isActive: false,
      isBuffered: false,
      isCanceled: false,
      isCompleted: false,
      isFailed: false,
      isPaused: false,
      isRunning: false,
      isStopped: false,
      isSuspended: false,
      originalStatus: "",
      refId: undefined,
      requisiteStageRefIds: undefined,
      runningTime: "",
      runningTimeInMs: 1000 * 60,
      startTime: new Date(2023, 0, 1, 1, 0, 0, 0).getTime(),
      status: 'RUNNING',
      tasks: [],
      type: "",
      outputs: {
        executionsList: [
          {
            id: testChildExecution.id,
            executionIdentifier: testParentExecution.id,
            startTime: testChildExecution.startTime,
            endTime: testChildExecution.startTime + 1000 * 60,
            status: 'SUCCEEDED',
            artifactCreated: {},
          },
        ],
      },
    };

    const detailsSections: IExecutionDetailsSection[] = [];

    render(
      <UIRouter plugins={[pushStateLocationPlugin]} states={[]}>
        <RunMultiplePipelinesStageExecutionDetails
          name="test_stage_execution"
          current="test_stage_execution"
          config={{
            key: 'test_type'
          }}
          application={testApplication}
          execution={testChildExecution}
          stage={stage}
          detailsSections={detailsSections}
          provider="test_provider"
        />
      </UIRouter>
    );

    const appLabel = screen.getByText('App');
    expect(appLabel).toBeInTheDocument();

    const startedLabel = screen.getByText('Started');
    expect(startedLabel).toBeInTheDocument();

    const durationLabel = screen.getByText('Duration');
    expect(durationLabel).toBeInTheDocument();

    const statusLabel = screen.getByText('Status');
    expect(statusLabel).toBeInTheDocument();

    const actionLabel = screen.getByText('Action');
    expect(actionLabel).toBeInTheDocument();

    const runningTimeInMs = screen.getByText('01:00');
    expect(runningTimeInMs).toBeInTheDocument();

    const status = screen.getByText('SUCCEEDED');
    expect(status).toBeInTheDocument();

    const rollbackAllLabel = screen.getByText('Rollback all apps');

    await userEvent.click(rollbackAllLabel);

    await waitFor(() => {
      const titleLabel = screen.getByText('Really perform Rollback of all apps?');
      expect(titleLabel).toBeInTheDocument();
    });

    const rollbackLabel = screen.getByText('Rollback');

    await userEvent.click(rollbackLabel);

    expect(PipelineConfigService.savePipeline).toBeCalled();
    expect(PipelineConfigService.triggerPipeline).toBeCalled();
  });
});
