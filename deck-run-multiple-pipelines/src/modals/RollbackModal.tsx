import React, { useEffect, useState } from 'react';

import { IPipeline, IStage, PipelineConfigService } from '@spinnaker/core';

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

    const [error ,setError] = useState("");
    const [loading ,setLoading] = useState("none");
    const [isDisabled, setDisabled] = useState(false);
    const [autoCloseModal, setAutoCloseModal] = useState(false);

    useEffect(() => {
        if (autoCloseModal===true && error==="") {
            props.setOpenModal(false);
        }
    }, [error, autoCloseModal]);

    (async function() {
        const deployStage = props.executionData.stages.find(function(stage: any) {
            return stage.name == "Deploy";
        });
        if (deployStage.outputs["outputs.createdArtifacts"] === undefined) {
            if (error === "") {
                setError("Can't perform rollback this pipeline did not create an artifact");
            }
            return;
        }
        account = deployStage.context["deploy.account.name"];
        manifestName = deployStage.outputs.manifests[0].kind + " " + deployStage.outputs["outputs.createdArtifacts"][0].name;
        location = deployStage.outputs["outputs.createdArtifacts"][0].location;

        if (rollbackPipelineId === "") {
            const pipelines = await PipelineConfigService.getPipelinesForApplication(props.executionData.application)
            pipelines.forEach((p: any) => {
                if (p.name == "rollbackOnFailure") {
                    rollbackPipelineId = p.id;
                }
            });

            if (rollbackPipelineId === "") {
                if (error === "") {
                    setError('Pipeline "rollbackOnFailure" not found create a pipeline with that name');
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
        setLoading("block");
        setDisabled(true);
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

            let triggerAfterSave = false;
            await PipelineConfigService.savePipeline(pipeline)
                .then(response => {
                    triggerAfterSave = true;
                    return response;
                })
                .catch( e => {
                    if (e.data.message == "null") {
                        setError("No details provided.");
                    } else {
                        setError(e.data.message);
                    }
                });

            if (triggerAfterSave) {
                const trigger = await PipelineConfigService.triggerPipeline(
                props.executionData.application, "rollbackOnFailure")
                    .then(response => {
                        return response;
                    }).catch(e => {
                        if (e.data.message == "null") {
                            setError("No details provided.");
                        } else {
                            setError(e.data.message);
                        }
                });
            }
        }
        setAutoCloseModal(true);
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
                        <p>This will perform rollback for the artifact created on {parameterAppName} Deploy stage.</p>
                    </div>
                    )}
                    {error!="" && (
                    <div className="modal-body" style={{color:"#bb231e"}}>
                        <h4>Error can not rollback</h4>
                        <p>{error}</p>
                    </div>
                    )}
                    <div className="modal-footer">
                        <button onClick={() => { props.setOpenModal(false);}} className="btn btn-default" type="button">Cancel</button>
                        <button onClick={ handleRollback } className="btn btn-primary"type="button" disabled={isDisabled}>
                            <div className="flex-container-h horizontal middle">
                                <svg xmlns="http://www.w3.org/2000/svg" style={{height:"16px", fill: "rgb(255, 255, 255)", display:loading}} preserveAspectRatio="xMidYMid" viewBox="24 24 52 52" display="block"><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.9166666666666666s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(30 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.8333333333333334s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(60 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.75s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(90 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.6666666666666666s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(120 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.5833333333333334s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(150 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.5s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(180 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.4166666666666667s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(210 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.3333333333333333s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(240 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.25s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(270 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.16666666666666666s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(300 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="-0.08333333333333333s" repeatCount="indefinite"></animate></rect><rect x="47" y="24" rx="3" ry="6" width="6" height="12" fill="current" transform="rotate(330 50 50)"><animate attributeName="opacity" values="1;0" keyTimes="0;1" dur="1s" begin="0s" repeatCount="indefinite"></animate></rect></svg>
                                {loading=="none" && <i className="far fa-check-circle"></i>}
                                <span className="sp-margin-xs-left">Rollback</span>
                            </div>
                        </button>
                    </div>
                </form>
            </div>
        </div>
     </div>
     </div>
    )
}

export default RollbackModal;
