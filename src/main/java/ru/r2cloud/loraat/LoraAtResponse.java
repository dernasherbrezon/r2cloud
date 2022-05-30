package ru.r2cloud.loraat;

import java.util.Collections;
import java.util.List;

public class LoraAtResponse {

	private ResponseStatus status;
	private String failureMessage;
	private List<LoraAtFrame> frames = Collections.emptyList();

	public LoraAtResponse() {
		// do nothing
	}

	public LoraAtResponse(String failureMessage) {
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

	public List<LoraAtFrame> getFrames() {
		return frames;
	}

	public void setFrames(List<LoraAtFrame> frames) {
		this.frames = frames;
	}

}
