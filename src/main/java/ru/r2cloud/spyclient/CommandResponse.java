package ru.r2cloud.spyclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface CommandResponse {

	void read(InputStream is) throws IOException;
	
	void write(OutputStream os) throws IOException;

}
