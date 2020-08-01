package ru.r2cloud.satellite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Map;
import java.util.regex.Pattern;

import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ProcessWrapper;

public class ProcessFactoryMock extends ProcessFactory {

	private static final Pattern SPACE = Pattern.compile("\\s");

	private final Map<String, ProcessWrapperMock> reply;
	private final String filePrefix;

	public ProcessFactoryMock(Map<String, ProcessWrapperMock> reply, String filePrefix) {
		this.reply = reply;
		this.filePrefix = filePrefix;
	}

	@Override
	public ProcessWrapper create(String commandLine, Redirect redirectError, boolean inheritIO) throws IOException {
		String[] parts = SPACE.split(commandLine);
		for (String cur : parts) {
			if (cur.contains(filePrefix)) {
				try (FileOutputStream fos = new FileOutputStream(new File(cur))) {
					fos.write(1);
				}
				break;
			}
		}
		return reply.get(parts[0]);
	}
}
