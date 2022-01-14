package ru.r2cloud.r2lora;

import java.util.Collections;
import java.util.List;

public class R2loraResponse {

	private ResponseStatus status;
	private String failureMessage;
	private List<R2loraFrame> frames = Collections.emptyList();

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
	
	public List<R2loraFrame> getFrames() {
		return frames;
	}
	
	public void setFrames(List<R2loraFrame> frames) {
		this.frames = frames;
	}

}
