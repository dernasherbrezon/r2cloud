package ru.r2cloud.sdrserver;

import java.io.DataInputStream;
import java.io.IOException;

public class SdrServerResponse {

	private MessageHeader header;
	private ResponseStatus status;
	private int details;

	public SdrServerResponse() {
		// do nothing
	}

	public SdrServerResponse(DataInputStream dis) throws IOException {
		header = new MessageHeader(dis);
		status = ResponseStatus.values()[dis.readUnsignedByte()];
		details = dis.readUnsignedByte();
	}

	public MessageHeader getHeader() {
		return header;
	}

	public void setHeader(MessageHeader header) {
		this.header = header;
	}

	public ResponseStatus getStatus() {
		return status;
	}

	public void setStatus(ResponseStatus status) {
		this.status = status;
	}

	public int getDetails() {
		return details;
	}

	public void setDetails(int details) {
		this.details = details;
	}

	@Override
	public String toString() {
		return "SdrServerResponse [header=" + header + ", status=" + status + ", details=" + details + "]";
	}

}
