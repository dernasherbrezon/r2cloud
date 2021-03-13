package ru.r2cloud.sdrserver;

import java.io.DataInputStream;
import java.io.IOException;

public class MessageHeader {

	private int protocol;
	private MessageType type;

	public MessageHeader() {
		// do nothing
	}

	public MessageHeader(DataInputStream dis) throws IOException {
		protocol = dis.readUnsignedByte();
		type = MessageType.values()[dis.readUnsignedByte()];
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "MessageHeader [protocol=" + protocol + ", type=" + type + "]";
	}

}
