package ru.r2cloud.satellite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
	
	private int defaultCode = 0;

	public ProcessFactoryMock(Map<String, ProcessWrapperMock> reply, String filePrefix) {
		this.reply = reply;
		this.filePrefix = filePrefix;
	}

	@Override
	public ProcessWrapper create(String commandLine, Redirect redirectError, boolean inheritIO) throws IOException {
		return handle(commandLine);
	}

	private ProcessWrapper handle(String commandLine) {
		String[] parts = SPACE.split(commandLine);
		ProcessWrapperMock result = reply.get(parts[0]);
		for (String cur : parts) {
			if (cur.contains(filePrefix)) {
				result.setBackingFile(new File(cur));
				break;
			}
		}
		return result;
	}

	@Override
	public ProcessWrapper create(String commandLine, boolean redirectErrorStream, boolean inheritIO) throws IOException {
		ProcessWrapper result = handle(commandLine);
		if (result == null) {
			return new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), defaultCode, false);
		}
		return result;
	}
	
	public void setDefaultCode(int defaultCode) {
		this.defaultCode = defaultCode;
	}
}
