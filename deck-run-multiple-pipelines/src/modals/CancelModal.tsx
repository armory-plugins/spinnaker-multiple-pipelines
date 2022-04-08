import React from "react";

declare global {
  interface Window {
    spinnaker?: any;
  }
}

function CancelModal(props: any) {

    const handleCancel = () => {
        window.spinnaker.executionService.cancelExecution(props.executionData.application, props.executionData.id);
        props.setOpenModal(false);
    }

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
                        <h4 className="modal-title">Really stop pipeline execution of {props.executionData.trigger.parameters.app}?</h4>
                    </div>
                    <div className="modal-footer">
                        <button onClick={() => { props.setOpenModal(false);}} className="btn btn-default" type="button">Cancel</button>
                        <button onClick={handleCancel} className="btn btn-primary" type="button"><div className="flex-container-h horizontal middle"><i className="far fa-check-circle"></i><span className="sp-margin-xs-left">Stop running pipeline</span></div></button>
                    </div>
                </form>
            </div>
        </div>
     </div>
     </div>
    )
}

export default CancelModal;