package ru.r2cloud.spyserver;

import java.io.IOException;
import java.io.InputStream;

public interface CommandResponse {

	void read(InputStream is) throws IOException;

}
