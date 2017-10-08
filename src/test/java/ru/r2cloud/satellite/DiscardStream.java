package ru.r2cloud.satellite;

import java.io.IOException;
import java.io.OutputStream;

public class DiscardStream extends OutputStream {

	@Override
	public void write(int b) throws IOException {
		// do nothing
	}
}
