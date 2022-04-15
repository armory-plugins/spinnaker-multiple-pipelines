import React, { useState } from 'react';

import { duration, ExecutionDetailsSection, IExecution, IExecutionDetailsSectionProps, timestamp, Tooltip } from '@spinnaker/core';

import CancelAllModal from './modals/CancelAllModal';
import CancelModal from './modals/CancelModal';
import RollbackAllAppsModal from './modals/RollbackAllAppsModal';
import RollbackModal from './modals/RollbackModal';

declare global {
  interface Window {
    spinnaker?: any;
  }
}

/*
 * You can use this component to provide information to users about
 * how the stage was configured and the results of its execution.
 *
 * In general, you will access two properties of `props.stage`:
 * - `props.stage.outputs` maps to your SimpleStage's `Output` class.
 * - `props.stage.context` maps to your SimpleStage's `Context` class.
 */
export function RunMultiplePipelinesStageExecutionDetails (props: IExecutionDetailsSectionProps) {
   const executionsSet = new Set();
   let doingAutoRollback = false;

   const [executionData, setExecutionData] = useState({});
   const [modalOpen, setModalOpen] = useState(false);
   const [rollbackModalOpen ,setRollbackModalOpen] = useState(false);
   const [rollbackAllAppsModalOpen ,setRollbackAllAppsModalOpen] = useState(false);
   const [cancelAllModalOpen, setCancelAllModalOpen] = useState(false);
   const [data ,setData] = useState([]);

   if (props.stage.outputs.executionsList == undefined) {
    props.stage.outputs.executionsList = [];
   }

   if (window.spinnaker) {
    const runningExecutions = window.spinnaker.application.runningExecutions.data;
    if (data != window.spinnaker.application.runningExecutions.data)
        setData(runningExecutions);
   }

   const {
        application,
        stage: { context = {} },
        stage: { outputs = {} },
        stage,
        name,
        current,
      } = props;

  const handleCancelClick = (execution: any) => async (e: any) => {
    setExecutionData({
        ...execution
    })
    await new Promise(f => setTimeout(f, 200));
    setModalOpen(true);
  }

  const handleRollbackClick = (execution: any) => async (e: any) => {
    setExecutionData({
        ...execution
    })
    await new Promise(f => setTimeout(f, 200));
    setRollbackModalOpen(true);
  }

  const handleAllRollbacksClick = (e: any) => {
    setRollbackAllAppsModalOpen(true);
  }

  const handleCancelAllClick = (e:any) => {
    setCancelAllModalOpen(true);
  }

  data.forEach( (execution: any) => {
    if (execution.trigger.correlationId != undefined) {
        if (execution.trigger.correlationId.includes(props.stage.id)) {
            executionsSet.add(execution);
            if (execution.name == "rollbackOnFailure") {
                doingAutoRollback = true;
            }
        }
    }
  });

  function findIfExecutionListCreatedArtifacts(executions:any) {
    for (const execution of executions) {
        const deployStage = execution.stages.find(function(stage: any) {
            return stage.name == "Deploy";
        });
        if (deployStage != undefined) {
            if (deployStage.outputs["outputs.createdArtifacts"] != undefined) {
                return true;
            }
        }
    }
    return false;
  }

    return (
      <ExecutionDetailsSection name={props.name} current={props.current}>
       <table className="table" style={{marginBottom: "0px"}}>
         <thead>
             <tr>
                 <th>App</th>
                 <th>Started</th>
                 <th>Duration</th>
                 <th>Status</th>
                 <th>Action</th>
             </tr>
         </thead>
         <tbody>
       {props.stage.outputs.executionsList.length === 0 && (
       <>
        {Array.from(executionsSet).map((execution: any, index: any) => {
            return (
             <tr className="clickable ng-scope info" analytics-on="click" analytics-category="Pipeline" key={execution.id}>
                 {execution.name != "rollbackOnFailure" &&
                    <td>{execution.trigger.parameters.app}</td>
                 }
                 {execution.name == "rollbackOnFailure" &&
                    <td>Rollback of {execution.stages[0].context.manifestName}</td>
                 }
                 <td className="ng-binding">{timestamp(execution.startTime)}</td>
                 <td className="ng-binding">{duration(execution.runningTimeInMs)}</td>
                 <td><span className={"label label-default label-" + execution.status.toLowerCase()}>{execution.status}</span></td>
                 {execution.name != "rollbackOnFailure" &&
                    <td><Tooltip value="Cancel execution">
                        <button className="link" onClick={handleCancelClick(execution)}>
                            <i style={{color:"#bb231e"}} className="far fa-times-circle" />
                        </button>
                    </Tooltip></td>
                 }
             </tr>
             );
        })}
       </>
       )}
         {props.stage.outputs.executionsList.map((execution: any, index: any) => {
            const deployStage = execution.stages.find(function(stage: any) {
                return stage.name == "Deploy";
            });
            return (
             <tr className="clickable ng-scope info" analytics-on="click" analytics-category="Pipeline" key={execution.id}>
                 <td>{execution.trigger.parameters.app}</td>
                 <td className="ng-binding">{timestamp(execution.startTime)}</td>
                 <td className="ng-binding">{duration(execution.endTime-execution.startTime)}</td>
                 <td><span className={"label label-default label-" + execution.status.toLowerCase()}>{execution.status}</span></td>
                 {deployStage.outputs["outputs.createdArtifacts"] != undefined &&
                    <td><Tooltip value="Rollback deploy">
                        <button className="link" onClick={handleRollbackClick(execution)}>
                            <i className="glyphicon glyphicon-backward"/>
                        </button>
                    </Tooltip></td>
                 }
             </tr>
             );
          })}
       </tbody>
       </table>
       {props.stage.outputs.executionsList.length === 0 &&
        <div style={{display: "flex",justifyContent: "flex-end"}}>
            <button onClick={handleCancelAllClick}>
                <span className="far fa-times-circle visible-lg-inline"></span>
                <span className="far fa-times-circle visible-md-inline visible-sm-inline"></span>
                <span className="visible-lg-inline"> Cancel all executions </span>
                </button>
        </div>
        }
       {props.stage.outputs.executionsList.length > 0 &&
       findIfExecutionListCreatedArtifacts(props.stage.outputs.executionsList) &&
        <div style={{display: "flex",justifyContent: "flex-end"}}>
            <button onClick={handleAllRollbacksClick}>
                <span className="glyphicon glyphicon-backward visible-lg-inline"></span>
                <span className="glyphicon glyphicon-backward visible-md-inline visible-sm-inline"></span>
                <span className="visible-lg-inline"> Rollback all apps </span>
                </button>
        </div>
        }
       {modalOpen && <CancelModal setOpenModal={setModalOpen} executionData={executionData}/>}
       {rollbackModalOpen && <RollbackModal setOpenModal={setRollbackModalOpen} executionData={executionData}/>}
       {rollbackAllAppsModalOpen && <RollbackAllAppsModal setOpenModal={setRollbackAllAppsModalOpen} allExecutions={props.stage.outputs.executionsList}/>}
       {cancelAllModalOpen && <CancelAllModal setOpenModal={setRollbackAllAppsModalOpen} allRunning={executionsSet}/>}
       {doingAutoRollback && props.stage.context.yamlConfig[0].bundle_web.rollback_onfailure === true &&
        <div>
            <p>Triggering rollbacks on failure..</p>
        </div>
       }
       <div style={{marginTop:"6px"}}>
        <p>rollback_onfailure is: {props.stage.context.yamlConfig[0].bundle_web.rollback_onfailure.toString()}</p>
       </div>
      </ExecutionDetailsSection>
    );
}

// eslint-disable-next-line
export namespace RunMultiplePipelinesStageExecutionDetails {
  export const title = 'pipelineConfig';
}
