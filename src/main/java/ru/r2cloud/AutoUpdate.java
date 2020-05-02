package ru.r2cloud;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class AutoUpdate {

	private static final Logger LOG = LoggerFactory.getLogger(AutoUpdate.class);
	private static final String FILE_LOCK = "DO_NOT_UPDATE";

	private final Path fileLock;

	public AutoUpdate(Configuration config) {
		Path basepath = config.getPathFromProperty("auto.update.basepath.location");
		if (!Util.initDirectory(basepath)) {
			throw new IllegalArgumentException("unable to init basepath: " + basepath);
		}
		fileLock = basepath.resolve(FILE_LOCK);
	}

	public boolean isEnabled() {
		return !Files.exists(fileLock);
	}

	public void setEnabled(boolean enabled) {
		if (!enabled && !Files.exists(fileLock)) {
			try {
				Files.createFile(fileLock);
			} catch (FileAlreadyExistsException e) {
				LOG.info("file already exists. skipping");
			} catch (IOException e) {
				LOG.error("unable to create file lock for auto update at: {}", fileLock.toAbsolutePath(), e);
			}
		}
		if (enabled && Files.exists(fileLock)) {
			try {
				Files.deleteIfExists(fileLock);
			} catch (IOException e) {
				LOG.error("unable to remove file lock for auto update at: {}", fileLock.toAbsolutePath(), e);
			}
		}
	}

}
