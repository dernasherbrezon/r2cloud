package ru.r2cloud.lora;

import java.util.Collections;
import java.util.List;

public class LoraResponse {

	private ResponseStatus status;
	private String failureMessage;
	private List<LoraFrame> frames = Collections.emptyList();

	public LoraResponse() {
		// do nothing
	}

	public LoraResponse(String failureMessage) {
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

	public List<LoraFrame> getFrames() {
		return frames;
	}

	public void setFrames(List<LoraFrame> frames) {
		this.frames = frames;
	}

}
