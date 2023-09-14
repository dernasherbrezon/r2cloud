package ru.r2cloud.util;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrateConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(MigrateConfiguration.class);
	private final Configuration config;

	public MigrateConfiguration(Configuration config) {
		this.config = config;
	}

	public void migrate() {
		String type = config.getProperty("satellites.sdr");
		if (type != null) {
			migrateSdrSettings(type.toUpperCase(Locale.UK));
			config.remove("satellites.sdr");
		}
		List<String> loraDevices = config.getProperties("r2lora.devices");
		if (!loraDevices.isEmpty()) {
			for (String cur : loraDevices) {
				String oldProp = "r2lora.device." + cur + ".hostport";
				String hostport = config.getProperty(oldProp);
				if (hostport != null) {
					int index = hostport.indexOf(':');
					if (index > 0) {
						config.setProperty("r2lora.device." + cur + ".host", hostport.substring(0, index));
						config.setProperty("r2lora.device." + cur + ".port", Integer.parseInt(hostport.substring(index + 1)));
						LOG.info("migrating: {} -> {} {}", oldProp, "r2lora.device." + cur + ".host", "r2lora.device." + cur + ".port");
					} else {
						continue;
					}
					config.remove(oldProp);
				}
			}
		}
		config.update();
	}

	private void migrateSdrSettings(String type) {
		List<String> sdrDevices = config.getProperties("sdr.devices");
		if (sdrDevices.isEmpty()) {
			sdrDevices = Collections.singletonList("0");
		}
		for (String cur : sdrDevices) {
			String prefix = "sdr.device." + cur + ".";
			String newPrefix = null;
			if (type.equals("RTLSDR")) {
				newPrefix = "rtlsdr.device." + cur + ".";
			} else if (type.equals("PLUTOSDR")) {
				newPrefix = "plutosdr.device." + cur + ".";
			} else if (type.equals("SDRSERVER")) {
				newPrefix = "sdrserver.device." + cur + ".";
			} else {
				continue;
			}
			migrate(newPrefix + "minFrequency", prefix + "minFrequency");
			migrate(newPrefix + "maxFrequency", prefix + "maxFrequency");
			if (type.equals("RTLSDR")) {
				migrate(newPrefix + "index", prefix + "rtlsdr.index", "satellites.rtlsdr.device.index");
				migrate(newPrefix + "biast", prefix + "rtlsdr.biast", "satellites.rtlsdr.biast");
				migrate(newPrefix + "ppm", prefix + "ppm", "ppm.current");
			}
			if (!type.equals("SDRSERVER")) {
				migrate(newPrefix + "gain", prefix + "rtlsdr.gain", "satellites.rtlsdr.gain");
			}
			migrate(newPrefix + "rotator.enabled", prefix + "rotator.enabled", "rotator.enabled");
			migrateRotatorConfiguration(prefix, newPrefix);
			if (type.equals("SDRSERVER")) {
				migrate(newPrefix + "host", prefix + "sdrserver.host", "satellites.sdrserver.host");
				migrate(newPrefix + "port", prefix + "sdrserver.port", "satellites.sdrserver.port");
				migrate("sdrserver.timeout", prefix + "sdrserver.timeout", "satellites.sdrserver.timeout");
				migrate(newPrefix + "basepath", prefix + "sdrserver.basepath");
				migrate(newPrefix + "usegzip", prefix + "sdrserver.usegzip");
			}
		}
	}

	private void migrateRotatorConfiguration(String prefix, String newPrefix) {
		migrate(newPrefix + "rotctrld.hostname", prefix + "rotctrld.hostname");
		migrate(newPrefix + "rotctrld.port", prefix + "rotctrld.port");
		migrate(newPrefix + "rotctrld.timeout", prefix + "rotctrld.timeout", "rotator.rotctrld.timeout");
		migrate(newPrefix + "rotator.tolerance", prefix + "rotator.tolerance", "rotator.tolerance");
		migrate(newPrefix + "rotator.cycleMillis", prefix + "rotator.cycleMillis", "rotator.cycleMillis");
	}

	private void migrate(String newname, String... oldNames) {
		String prop = null;
		for (String cur : oldNames) {
			prop = config.getProperty(cur);
			if (prop != null) {
				// log all possible migrations
				LOG.info("migrating: {} -> {}", cur, newname);
				config.remove(cur);
			}
		}
		if (prop == null) {
			return;
		}
		config.setProperty(newname, prop);
	}

}
