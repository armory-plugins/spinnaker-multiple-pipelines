import React, { useState } from 'react';

import {PipelineConfigService,
        IPipeline,
        IStage} from '@spinnaker/core';

declare global {
  interface Window {
    spinnaker?: any;
  }
}

function RollbackModal(props: any) {
    let rollbackPipelineId = "";
    const parameterAppName = props.executionData.trigger.parameters.app;
    let account = "";
    let manifestName = "";
    let location = "";

    const [error ,serError] = useState("");

    (async function() {
        const deployStage = props.executionData.stages.find(function(stage: any) {
            return stage.name == "Deploy";
        });
        if (deployStage.outputs["outputs.createdArtifacts"] === undefined) {
            if (error === "") {
                serError("Can't perform rollback this pipeline did not create an artifact");
            }
            return;
        }
        account = deployStage.context["deploy.account.name"];
        manifestName = deployStage.outputs.manifests[0].kind + " " + deployStage.outputs["outputs.createdArtifacts"][0].name;
        location = deployStage.outputs["outputs.createdArtifacts"][0].location;

        if (rollbackPipelineId === "") {
            PipelineConfigService.getPipelinesForApplication(props.executionData.application)
                .then((pipelines: any) => {
                    pipelines.forEach((p: any) => {
                        if (p.name == "rollbackOnFailure") {
                            rollbackPipelineId = p.id;
                        }
                    });
                return pipelines;
            });
            //wait 300 ms to get the response back
            await new Promise(f => setTimeout(f, 300));
            if (rollbackPipelineId === "") {
                if (error === "") {
                    serError('Pipeline "rollbackOnFailure" not found create a pipeline with that name');
                }
            }
        }
    }());

    const stage: IStage = {
        "account": account,
        "manifestName": manifestName,
        "location": location,
        "numRevisionsBack": 1,
        "cloudProvider": "kubernetes",
        "mode": "static",
        name: "Undo Rollout (Manifest) " + parameterAppName,
        refId: "1", // unfortunately, we kept this loose early on, so it's either a string or a number
        requisiteStageRefIds: [],
        type: "undoRolloutManifest"
    };

    const handleRollback = async () => {
        if (error === "") {
           const stagesArray = [];
           stagesArray.push(stage);
            const pipeline: IPipeline = {
                  application: props.executionData.application,
                  id: rollbackPipelineId,
                  keepWaitingPipelines: false,
                  limitConcurrent: false,
                  name: "rollbackOnFailure",
                  stages: stagesArray,
                  triggers: [],
                  parameterConfig: []
            };
            PipelineConfigService.savePipeline(pipeline);
            await new Promise(f => setTimeout(f, 200));
            const trigger = PipelineConfigService.triggerPipeline(
            props.executionData.application, "rollbackOnFailure");
            props.setOpenModal(false);
        }
    };

    return (
     <div role="dialog">
     <div className="fade modal-backdrop in"></div>
     <div role="dialog" className="fade in modal" style={{display:"block"}}>
        <div className="modal-dialog">
            <div className="modal-content" role="document">
                <form className="form-horizontal">
                    <div className="modal-close close-button pull-right" style={{marginTop:"4px", marginRight:"4px"}}>
                        <button
                            onClick={() => { props.setOpenModal(false);}}
                            className="link" type="button">
                            <span className="glyphicon glyphicon-remove"></span>
                        </button>
                    </div>
                    <div className="modal-header">
                        <h4 className="modal-title">Really perform Rollback of {parameterAppName}?</h4>
                    </div>
                    {error==="" && (
                    <div className="modal-body">
                        <p>This will perform rollback for the artifact created on stage named Deploy.</p>
                    </div>
                    )}
                    {error!="" && (
                    <div className="modal-body" style={{color:"#bb231e"}}>
                        <h4>Error can not Rollback</h4>
                        <p>{error}</p>
                    </div>
                    )}
                    <div className="modal-footer">
                        <button onClick={() => { props.setOpenModal(false);}} className="btn btn-default" type="button">Cancel</button>
                        <button onClick={ handleRollback } className="btn btn-primary"type="button"><div className="flex-container-h horizontal middle"><i className="far fa-check-circle"></i><span className="sp-margin-xs-left">Rollback</span></div></button>
                    </div>
                </form>
            </div>
        </div>
     </div>
     </div>
    )
}

export default RollbackModal;
