package ru.r2cloud.spyserver;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CommandHello implements CommandRequest {

	private int protocolVersion;
	private String clientId;

	public CommandHello(int protocolVersion, String clientId) {
		super();
		this.protocolVersion = protocolVersion;
		this.clientId = clientId;
	}

	public int getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(int protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@Override
	public byte[] toByteArray() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			LittleEndianDataOutputStream dis = new LittleEndianDataOutputStream(new DataOutputStream(baos));
			dis.writeInt(protocolVersion);
			dis.write(clientId.getBytes(StandardCharsets.US_ASCII));
			dis.flush();
			return baos.toByteArray();
		} catch (IOException e) {
			return new byte[0];
		}
	}

}
