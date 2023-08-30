package ru.r2cloud.spyserver;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import ru.r2cloud.jradio.util.LittleEndianDataInputStream;

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

	public static ResponseHeader read(InputStream is) throws IOException {
		LittleEndianDataInputStream dis = new LittleEndianDataInputStream(new DataInputStream(is));
		ResponseHeader result = new ResponseHeader();
		result.protocolID = dis.readUnsignedInt();
		long mTypeRaw = dis.readUnsignedInt();
		result.messageType = (int) (mTypeRaw & 0xFFFF);
		result.flags = (int) (mTypeRaw & 0xFFFF0000) >> 16;
		result.streamType = dis.readUnsignedInt();
		result.sequenceNumber = dis.readUnsignedInt();
		result.bodySize = dis.readUnsignedInt();
		return result;
	}

}
