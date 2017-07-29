package ru.r2cloud.uitl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.util.logging.XMLFormatter;

public class DailyRollingFileHandler extends StreamHandler {

	private String pattern;
	private String scheduledFilename;
	private Calendar nextRoll;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	public DailyRollingFileHandler() {
		configure();
		setOutputStream();
	}

	private void setOutputStream() {
		File file = new File(pattern);
		File parent = file.getParentFile();
		if (!parent.exists() && !parent.mkdirs()) {
			throw new RuntimeException("unable to setup file");
		}
		try {
			setOutputStream(new FileOutputStream(file, true));
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void configure() {
		LogManager manager = LogManager.getLogManager();

		String cname = getClass().getName();

		setTime(Calendar.getInstance());
		setPattern(getStringProperty(manager, cname + ".pattern", null));
		setLevel(getLevelProperty(manager, cname + ".level", Level.ALL));
		setFilter(getFilterProperty(manager, cname + ".filter", null));
		setFormatter(getFormatterProperty(manager, cname + ".formatter", new XMLFormatter()));
		try {
			setEncoding(getStringProperty(manager, cname + ".encoding", null));
		} catch (Exception ex) {
			try {
				setEncoding(null);
			} catch (Exception ex2) {
				// doing a setEncoding with null should always work.
				// assert false;
			}
		}

	}

	@Override
	public synchronized void publish(LogRecord record) {
		if (!isLoggable(record)) {
			return;
		}
		if (System.currentTimeMillis() >= nextRoll.getTimeInMillis()) {
			rollOver();
		}
		super.publish(record);
		flush();
	}

	private void rollOver() {

		String datedFilename = pattern + "." + sdf.format(new Date());
		// It is too early to roll over because we are still within the
		// bounds of the current interval. Rollover will occur once the
		// next interval is reached.
		if (scheduledFilename.equals(datedFilename)) {
			return;
		}

		// close current file, and rename it to datedFilename
		close();

		File target = new File(scheduledFilename);
		if (target.exists() && !target.delete()) {
			System.err.println("unable to delete previous: " + target.getAbsolutePath());
		}

		File file = new File(pattern);
		boolean result = file.renameTo(target);
		if (!result) {
			System.err.println("unable to rename to: " + target.getAbsolutePath());
		}

		setOutputStream();
		scheduledFilename = datedFilename;
		nextRoll.add(Calendar.DAY_OF_MONTH, 1);
	}

	// used in test
	void setTime(Calendar cal) {
		nextRoll = Calendar.getInstance();
		nextRoll.set(Calendar.HOUR_OF_DAY, 0);
		nextRoll.set(Calendar.MINUTE, 0);
		nextRoll.set(Calendar.SECOND, 0);
		nextRoll.set(Calendar.MILLISECOND, 0);

		if (nextRoll.before(cal)) {
			nextRoll.add(Calendar.DAY_OF_MONTH, 1);
		}
	}

	private void setPattern(String pattern) {
		this.pattern = pattern;
		scheduledFilename = pattern + "." + sdf.format(new Date());
	}

	private static String getStringProperty(LogManager manager, String name, String defaultValue) {
		String val = manager.getProperty(name);
		if (val == null) {
			return defaultValue;
		}
		return val.trim();
	}

	private static Level getLevelProperty(LogManager manager, String name, Level defaultValue) {
		String val = manager.getProperty(name);
		if (val == null) {
			return defaultValue;
		}
		Level l = Level.parse(val.trim());
		return l != null ? l : defaultValue;
	}

	private static Filter getFilterProperty(LogManager manager, String name, Filter defaultValue) {
		String val = manager.getProperty(name);
		try {
			if (val != null) {
				Class<?> clz = ClassLoader.getSystemClassLoader().loadClass(val);
				return (Filter) clz.newInstance();
			}
		} catch (Exception ex) {
			// We got one of a variety of exceptions in creating the
			// class or creating an instance.
			// Drop through.
		}
		// We got an exception. Return the defaultValue.
		return defaultValue;
	}

	private static Formatter getFormatterProperty(LogManager manager, String name, Formatter defaultValue) {
		String val = manager.getProperty(name);
		try {
			if (val != null) {
				Class<?> clz = ClassLoader.getSystemClassLoader().loadClass(val);
				return (Formatter) clz.newInstance();
			}
		} catch (Exception ex) {
			// We got one of a variety of exceptions in creating the
			// class or creating an instance.
			// Drop through.
		}
		// We got an exception. Return the defaultValue.
		return defaultValue;
	}
}
