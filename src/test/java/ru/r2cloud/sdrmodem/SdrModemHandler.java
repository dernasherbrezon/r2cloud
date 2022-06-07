package ru.r2cloud.sdrmodem;

import java.io.IOException;
import java.net.Socket;

public interface SdrModemHandler {

	void handleClient(Socket client) throws IOException;
	
}
