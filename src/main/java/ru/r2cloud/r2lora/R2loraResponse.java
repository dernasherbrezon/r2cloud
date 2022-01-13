package ru.r2cloud.r2lora;

public class R2loraResponse {

	private ResponseStatus status;
	private String failureMessage;

	public R2loraResponse() {
		// do nothing
	}

	public R2loraResponse(String failureMessage) {
		status = ResponseStatus.FAILURE;
		this.failureMessage = failureMessage;
	}

	public ResponseStatus getStatus() {
		return status;
	}

	public void setStatus(ResponseStatus status) {
		this.status = status;
	}

	public String getFailureMessage() {
		return failureMessage;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

}
