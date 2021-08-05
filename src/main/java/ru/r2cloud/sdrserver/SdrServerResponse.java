package ru.r2cloud.sdrserver;

import java.io.DataInputStream;
import java.io.IOException;

import ru.r2cloud.jradio.util.StreamUtils;

public class SdrServerResponse {

	private MessageHeader header;
	private ResponseStatus status;
	private long details;

	public SdrServerResponse() {
		// do nothing
	}

	public SdrServerResponse(DataInputStream dis) throws IOException {
		header = new MessageHeader(dis);
		status = ResponseStatus.values()[dis.readUnsignedByte()];
		details = StreamUtils.readUnsignedInt(dis);
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

	public long getDetails() {
		return details;
	}

	public void setDetails(long details) {
		this.details = details;
	}

	@Override
	public String toString() {
		return "SdrServerResponse [header=" + header + ", status=" + status + ", details=" + details + "]";
	}

}
