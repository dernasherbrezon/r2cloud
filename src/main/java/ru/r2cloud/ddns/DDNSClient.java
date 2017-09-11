package ru.r2cloud.ddns;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;

public class DDNSClient {

	private final static Logger LOG = LoggerFactory.getLogger(DDNSClient.class);

	private final Configuration config;

	private ScheduledExecutorService executor;
	private Runnable task;

	public DDNSClient(Configuration config) {
		this.config = config;
	}

	public void start() {
		String typeStr = config.getProperty("ddns.type");
		if (typeStr == null || typeStr.trim().length() == 0) {
			return;
		}
		try {
			DDNSType type = DDNSType.valueOf(typeStr);
			switch (type) {
			case NONE:
				LOG.info("ddns is disabled");
				return;
			case NOIP:
				task = new NoIPTask(config);
				LOG.info("ddns provider is no-ip");
				break;
			default:
				throw new IllegalArgumentException("unsupported ddns provider: " + type);
			}
		} catch (Exception e) {
			LOG.error("unable to configure ddns", e);
			return;
		}

		executor = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("ddns-updater"));
		executor.scheduleAtFixedRate(task, 0, config.getLong("ddns.interval.seconds"), TimeUnit.SECONDS);
	}

	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
	}

}
