package ru.r2cloud.spyclient;

import java.io.IOException;
import java.io.InputStream;

public interface CommandResponse {

	void read(InputStream is) throws IOException;

}
