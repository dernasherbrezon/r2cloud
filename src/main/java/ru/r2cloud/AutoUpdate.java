package ru.r2cloud;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class AutoUpdate {

	private static final Logger LOG = LoggerFactory.getLogger(AutoUpdate.class);
	private static final String FILE_LOCK = "DO_NOT_UPDATE";

	private final File basepath;

	public AutoUpdate(Configuration config) {
		basepath = Util.initDirectory(config.getProperty("auto.update.basepath.location"));
	}

	public boolean isEnabled() {
		return !new File(basepath, FILE_LOCK).exists();
	}

	public void setEnabled(boolean enabled) {
		File fileLock = new File(basepath, FILE_LOCK);
		if (!enabled && !fileLock.exists()) {
			try {
				if (!fileLock.createNewFile()) {
					LOG.error("unable to create file lock for auto update at: {}", fileLock.getAbsolutePath());
				}
			} catch (IOException e) {
				LOG.error("unable to create file lock for auto update at: {}", fileLock.getAbsolutePath(), e);
			}
		}
		if (enabled && fileLock.exists() && !fileLock.delete()) {
			LOG.error("unable to remove file lock for auto update at: {}", fileLock.getAbsolutePath());
		}
	}

}
