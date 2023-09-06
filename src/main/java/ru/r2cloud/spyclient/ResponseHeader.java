package ru.r2cloud.spyclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ResponseHeader {

	private long protocolID;
	private int messageType;
	private int flags;
	private long streamType;
	private long sequenceNumber;
	private long bodySize;

	public long getProtocolID() {
		return protocolID;
	}

	public void setProtocolID(long protocolID) {
		this.protocolID = protocolID;
	}

	public int getMessageType() {
		return messageType;
	}

	public void setMessageType(int messageType) {
		this.messageType = messageType;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public long getStreamType() {
		return streamType;
	}

	public void setStreamType(long streamType) {
		this.streamType = streamType;
	}

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public long getBodySize() {
		return bodySize;
	}

	public void setBodySize(long bodySize) {
		this.bodySize = bodySize;
	}

	public void read(InputStream is) throws IOException {
		protocolID = SpyClient.readUnsignedInt(is);
		long mTypeRaw = SpyClient.readUnsignedInt(is);
		messageType = (int) (mTypeRaw & 0xFFFF);
		flags = (int) (mTypeRaw & 0xFFFF0000) >> 16;
		streamType = SpyClient.readUnsignedInt(is);
		sequenceNumber = SpyClient.readUnsignedInt(is);
		bodySize = SpyClient.readUnsignedInt(is);
	}

	public void write(OutputStream os) throws IOException {
		SpyClient.writeUnsignedInt(os, (int) protocolID);
		long mtypeRaw = (flags << 16) + messageType;
		SpyClient.writeUnsignedInt(os, (int) mtypeRaw);
		SpyClient.writeUnsignedInt(os, (int) streamType);
		SpyClient.writeUnsignedInt(os, (int) sequenceNumber);
		SpyClient.writeUnsignedInt(os, (int) bodySize);
	}

}
