package ru.r2cloud.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.SdrType;

public class MigrateConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(MigrateConfiguration.class);
	private final Configuration config;

	public MigrateConfiguration(Configuration config) {
		this.config = config;
	}

	public void migrate() {
		SdrType type = config.getSdrType();
		if (type != null) {
			migrateSdrSettings(type);
			config.remove("satellites.sdr");
		}
		config.update();
	}

	private void migrateSdrSettings(SdrType type) {
		List<String> sdrDevices = config.getProperties("sdr.devices");
		if (sdrDevices.isEmpty()) {
			sdrDevices = Collections.singletonList("0");
		}
		List<DeviceConfiguration> result = new ArrayList<>(sdrDevices.size());
		for (String cur : sdrDevices) {
			String prefix = "sdr.device." + cur + ".";
			String newPrefix = null;
			if (type.equals(SdrType.RTLSDR)) {
				newPrefix = "rtlsdr.device." + cur + ".";
			} else if (type.equals(SdrType.PLUTOSDR)) {
				newPrefix = "plutosdr.device." + cur + ".";
			} else if (type.equals(SdrType.SDRSERVER)) {
				newPrefix = "sdrserver.device." + cur + ".";
			} else {
				continue;
			}
			migrate(newPrefix + "minFrequency", prefix + "minFrequency");
			migrate(newPrefix + "maxFrequency", prefix + "maxFrequency");
			if (type.equals(SdrType.RTLSDR)) {
				migrate(newPrefix + "index", prefix + "rtlsdr.index", "satellites.rtlsdr.device.index");
				migrate(newPrefix + "biast", prefix + "rtlsdr.biast", "satellites.rtlsdr.biast");
				migrate(newPrefix + "ppm", prefix + "ppm", "ppm.current");
			}
			if (!type.equals(SdrType.SDRSERVER)) {
				migrate(newPrefix + "gain", prefix + "rtlsdr.gain", "satellites.rtlsdr.gain");
			}
			migrate(newPrefix + "rotator.enabled", prefix + "rotator.enabled", "rotator.enabled");
			migrateRotatorConfiguration(prefix, newPrefix);
			if (type.equals(SdrType.SDRSERVER)) {
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
