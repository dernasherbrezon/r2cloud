package ru.r2cloud.ssl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

class Messages {
	
	private final List<String> messages = new ArrayList<String>();
	
	public synchronized void add(String message) {
		messages.add(message);
	}

	public synchronized void add(String message, Logger log) {
		log.info(message);
		messages.add(message);
	}

	public synchronized void clear() {
		messages.clear();
	}
	
	public synchronized List<String> get() {
		return new ArrayList<String>(messages);
	}

}
