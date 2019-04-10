package ru.r2cloud.util;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessFactory {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessFactory.class);

	private static final Pattern SPACE = Pattern.compile("\\s");

	public ProcessWrapper create(String commandLine, Redirect redirectError, boolean inheritIO) throws IOException {
		LOG.info("started with arguments: {}", commandLine);
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
		List<String> args = new ArrayList<>();
		for (String cur : SPACE.split(commandLine)) {
			args.add(cur);
		}
		return create(args, redirectErrorStream, inheritIO);
	}

	public ProcessWrapper create(List<String> commandLine, boolean redirectErrorStream, boolean inheritIO) throws IOException {
		LOG.info("started with arguments: {}", commandLine);
		ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
		processBuilder.redirectErrorStream(redirectErrorStream);
		if (inheritIO) {
			processBuilder.inheritIO();
		}
		return new ProcessWrapperImpl(processBuilder.start());
	}

}
