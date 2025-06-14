package ru.r2cloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import ru.r2cloud.satellite.ProcessWrapperMock;

public class WxtoimgProcessMock extends ProcessWrapperMock {

	private final String response;

	public WxtoimgProcessMock(String response) {
		super(null, null);
		this.response = response;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(response.getBytes(StandardCharsets.US_ASCII));
	}

	@Override
	public OutputStream getOutputStream() {
		return new ByteArrayOutputStream();
	}
}
