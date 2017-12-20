package ru.r2cloud.util;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.regex.Pattern;

public class ProcessFactory {
	
	private final static Pattern SPACE = Pattern.compile("\\s");

	public ProcessWrapper create(String commandLine, Redirect redirectError, boolean inheritIO) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder(SPACE.split(commandLine));
		if (redirectError != null) {
			processBuilder.redirectError(redirectError);
		}
		if (inheritIO) {
			processBuilder.inheritIO();
		}
		return new ProcessWrapperImpl(processBuilder.start());
	}
	
	public ProcessWrapper create(String commandLine, boolean redirectErrorStream, boolean inheritIO) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder(SPACE.split(commandLine));
		processBuilder.redirectErrorStream(redirectErrorStream);
		if (inheritIO) {
			processBuilder.inheritIO();
		}
		return new ProcessWrapperImpl(processBuilder.start());
	}

}
