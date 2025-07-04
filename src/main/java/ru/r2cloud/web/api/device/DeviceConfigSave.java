package ru.r2cloud.web.api.device;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.AirspyGainType;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.SdrServerConfiguration;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class DeviceConfigSave extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(DeviceConfigSave.class);
	private final Configuration props;
	private final DeviceManager manager;

	public DeviceConfigSave(Configuration config, DeviceManager manager) {
		this.props = config;
		this.manager = manager;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		ValidationResult errors = new ValidationResult();
		DeviceType deviceType = readDeviceType(request, errors);
		// fail fast first to reduce complexity of the code below.
		// no need to check if device type is not null
		if (deviceType == null) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}
		DeviceConfiguration config = new DeviceConfiguration();
		config.setDeviceType(deviceType);
		config.setId(request.getString("id", null));
		Device device = manager.findDeviceById(config.getId());
		if (config.getId() != null && device == null) {
			errors.setGeneral("Unknown device id");
		}
		config.setMinimumFrequency(readLong(request, "minimumFrequency", errors));
		config.setMaximumFrequency(readLong(request, "maximumFrequency", errors));
		if (config.getMinimumFrequency() > config.getMaximumFrequency()) {
			errors.put("minimumFrequency", "Cannot be more than maximum frequency");
		}
		switch (deviceType) {
		case RTLSDR: {
			config.setRtlDeviceId(request.getString("rtlDeviceId", "0"));
			config.setGain((float) readPositiveDouble(request, "gain", errors, 49.6));
			config.setBiast(WebServer.getBoolean(request, "biast"));
			config.setPpm((int) readOptionalLong(request, "ppm", 0, errors));
			if (config.getMaximumFrequency() > 1_766_000_000L) {
				errors.put("maximumFrequency", "RTL-SDR doesn't support more than 1766 Mhz");
			}
			if (config.getMinimumFrequency() < 500_000L) {
				errors.put("minimumFrequency", "RTL-SDR doesn't support less than 500 Khz");
			}
			break;
		}
		case AIRSPY: {
			config.setRtlDeviceId(request.getString("rtlDeviceId", null));
			config.setBiast(WebServer.getBoolean(request, "biast"));
			String gainType = request.getString("gainType", null);
			if (gainType == null) {
				errors.put("gainType", "Cannot be null");
			} else {
				try {
					config.setGainType(AirspyGainType.valueOf(gainType));
					switch (config.getGainType()) {
					case FREE: {
						config.setVgaGain((float) readPositiveDouble(request, "vgaGain", errors, 15));
						config.setMixerGain((float) readPositiveDouble(request, "mixerGain", errors, 15));
						config.setLnaGain((float) readPositiveDouble(request, "lnaGain", errors, 14));
						break;
					}
					case LINEAR: {
						config.setGain((float) readPositiveDouble(request, "gain", errors, 21));
						break;
					}
					case SENSITIVE: {
						config.setGain((float) readPositiveDouble(request, "gain", errors, 21));
						break;
					}
					}
				} catch (Exception e) {
					errors.put("gainType", "Unsupported gain type: " + gainType);
				}
			}
			if (config.getMaximumFrequency() > 1_750_000_000L) {
				errors.put("maximumFrequency", "AIRSPY doesn't support more than 1750 Mhz");
			}
			if (config.getMinimumFrequency() < 24_000_000L) {
				errors.put("minimumFrequency", "AIRSPY doesn't support less than 24 Mhz");
			}
			break;
		}
		case LORAAT: {
			config.setSerialDevice(WebServer.getString(request, "serialDevice"));
			if (config.getSerialDevice() == null) {
				errors.put("serialDevice", Messages.CANNOT_BE_EMPTY);
			}
			config.setGain(readLong(request, "gain", errors));
			if (config.getGain() > 6) {
				errors.put("gain", "Cannot be more than 6");
			}
			break;
		}
		case LORAATBLEC: {
			config.setBtAddress((WebServer.getString(request, "btAddress")));
			if (config.getBtAddress() == null) {
				errors.put("btAddress", Messages.CANNOT_BE_EMPTY);
			}
			config.setGain(readLong(request, "gain", errors));
			if (config.getGain() > 6) {
				errors.put("gain", "Cannot be more than 6");
			}
			break;
		}
		case LORAATBLE: {
			config.setBtAddress((WebServer.getString(request, "btAddress")));
			if (config.getBtAddress() == null) {
				errors.put("btAddress", Messages.CANNOT_BE_EMPTY);
			}
			config.setGain(readLong(request, "gain", errors));
			if (config.getGain() > 6) {
				errors.put("gain", "Cannot be more than 6");
			}
			config.setMinimumBatteryVoltage(readOptionalPositiveDouble(request, "minimumBatteryVoltage", 3.0, errors));
			config.setMaximumBatteryVoltage(readOptionalPositiveDouble(request, "maximumBatteryVoltage", 4.2, errors));
			if (config.getMinimumBatteryVoltage() > config.getMaximumBatteryVoltage()) {
				errors.put("minimumBatteryVoltage", "Cannot be more than maximum voltage");
			}
			break;
		}
		case LORAATWIFI: {
			config.setHost(readHost(request, "host", errors));
			config.setPort((int) readLong(request, "port", errors));
			config.setGain(readLong(request, "gain", errors));
			if (config.getGain() > 6) {
				errors.put("gain", "Cannot be more than 6");
			}
			config.setUsername(WebServer.getString(request, "username"));
			if (config.getUsername() == null) {
				errors.put("username", Messages.CANNOT_BE_EMPTY);
			}
			config.setPassword(WebServer.getString(request, "password"));
			if (config.getPassword() == null) {
				errors.put("password", Messages.CANNOT_BE_EMPTY);
			}
			break;
		}
		case PLUTOSDR: {
			config.setGain((float) readPositiveDouble(request, "gain", errors, 70));
			break;
		}
		case SDRSERVER: {
			config.setHost(readHost(request, "host", errors));
			config.setPort((int) readLong(request, "port", errors));
			config.setSdrServerConfiguration(readSdrServerConfiguration(request, errors));
			break;
		}
		case SPYSERVER: {
			config.setHost(readHost(request, "host", errors));
			config.setPort((int) readLong(request, "port", errors));
			config.setGain((float) readPositiveDouble(request, "gain", errors, 100)); // 100 is the max
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + deviceType);
		}
		JsonValue antennaValue = request.get("antenna");
		if (antennaValue != null && antennaValue.isObject()) {
			ValidationResult antennaConfigValidaiton = new ValidationResult();
			config.setAntennaConfiguration(readAntennaConfiguration(antennaValue.asObject(), antennaConfigValidaiton));
			errors.appendWithPrefix("antenna.", antennaConfigValidaiton);
		} else {
			errors.put("antenna.antennaType", Messages.CANNOT_BE_EMPTY);
		}
		if (config.getAntennaConfiguration() != null && AntennaType.DIRECTIONAL.equals(config.getAntennaConfiguration().getType())) {
			JsonValue rotatorValue = request.get("rotator");
			if (rotatorValue != null && rotatorValue.isObject()) {
				ValidationResult rotatorValidation = new ValidationResult();
				config.setRotatorConfiguration(readRotatorConfiguration(rotatorValue.asObject(), rotatorValidation));
				errors.appendWithPrefix("rotator.", rotatorValidation);
			} else {
				errors.put("rotator.rotctrldHostname", Messages.CANNOT_BE_EMPTY);
			}
		}
		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}
		props.saveDeviceConfiguration(config);
		props.update();

		JsonObject json = new JsonObject();
		json.add("id", config.getId());
		ModelAndView result = new ModelAndView();
		result.setData(json.toString());
		return result;
	}

	private static SdrServerConfiguration readSdrServerConfiguration(JsonObject request, ValidationResult errors) {
		SdrServerConfiguration result = new SdrServerConfiguration();
		result.setBandwidth(readLong(request, "bandwidth", errors));
		result.setBandwidthCrop(readLong(request, "bandwidthCrop", errors));
		result.setBasepath(WebServer.getString(request, "basepath"));
		if (result.getBasepath() == null) {
			errors.put("basepath", Messages.CANNOT_BE_EMPTY);
		}
		result.setUseGzip(WebServer.getBoolean(request, "usegzip"));
		return result;
	}

	private static RotatorConfiguration readRotatorConfiguration(JsonObject request, ValidationResult errors) {
		RotatorConfiguration result = new RotatorConfiguration();
		result.setHostname(readHost(request, "rotctrldHostname", errors));
		result.setPort((int) readLong(request, "rotctrldPort", errors));
		result.setTolerance(readPositiveDouble(request, "rotatorTolerance", errors, 360));
		result.setCycleMillis((int) readLong(request, "rotatorCycle", errors));
		return result;
	}

	private static AntennaConfiguration readAntennaConfiguration(JsonObject request, ValidationResult errors) {
		AntennaType antennaType = readAntennaType(request, errors);
		if (antennaType == null) {
			return null;
		}
		AntennaConfiguration result = new AntennaConfiguration();
		result.setType(antennaType);
		switch (antennaType) {
		case OMNIDIRECTIONAL:
		case DIRECTIONAL: {
			result.setMinElevation(readPositiveDouble(request, "minElevation", errors, 90.0));
			result.setGuaranteedElevation(readPositiveDouble(request, "guaranteedElevation", errors, 90.0));
			if (result.getMinElevation() > result.getGuaranteedElevation()) {
				errors.put("minElevation", "Cannot be more than guaranteed elevation");
			}
			break;
		}
		case FIXED_DIRECTIONAL: {
			result.setAzimuth(readPositiveDouble(request, "azimuth", errors, 360.0f));
			result.setElevation(readPositiveDouble(request, "elevation", errors, 90.0));
			result.setBeamwidth(readPositiveDouble(request, "beamwidth", errors, 360.0f));
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + antennaType);
		}
		return result;
	}

	private static AntennaType readAntennaType(JsonObject request, ValidationResult errors) {
		String valueStr = request.getString("antennaType", null);
		if (valueStr == null) {
			errors.put("antenna.antennaType", Messages.CANNOT_BE_EMPTY);
			return null;
		}
		try {
			return AntennaType.valueOf(valueStr);
		} catch (Exception e) {
			errors.put("antenna.antennaType", "unsupported");
			return null;
		}
	}

	private static DeviceType readDeviceType(JsonObject request, ValidationResult errors) {
		String deviceTypeStr = request.getString("deviceType", null);
		if (deviceTypeStr == null) {
			errors.put("deviceType", Messages.CANNOT_BE_EMPTY);
			return null;
		}
		try {
			return DeviceType.valueOf(deviceTypeStr);
		} catch (Exception e) {
			errors.put("deviceType", "unsupported");
			return null;
		}
	}

	private static long readLong(JsonObject request, String name, ValidationResult errors) {
		JsonValue value = request.get(name);
		if (value == null) {
			errors.put(name, Messages.CANNOT_BE_EMPTY);
			// doesn't matter what return value is - the request will fail
			return 0;
		}
		if (!value.isNumber()) {
			errors.put(name, "invalid value");
			return 0;
		}
		return value.asLong();
	}

	private static String readHost(JsonObject request, String name, ValidationResult errors) {
		String result = WebServer.getString(request, name);
		if (result == null) {
			errors.put(name, Messages.CANNOT_BE_EMPTY);
			return null;
		} else {
			try {
				InetAddress.getByName(result);
				return result;
			} catch (Exception e) {
				errors.put(name, "invalid hostname");
				return null;
			}
		}
	}

	private static long readOptionalLong(JsonObject request, String name, long defaultValue, ValidationResult errors) {
		JsonValue value = request.get(name);
		if (value == null) {
			return defaultValue;
		}
		if (!value.isNumber()) {
			errors.put(name, "invalid value");
			return 0;
		}
		return value.asLong();
	}

	private static double readOptionalPositiveDouble(JsonObject requrst, String name, double defaultValue, ValidationResult errors) {
		JsonValue value = requrst.get(name);
		if (value == null) {
			return defaultValue;
		}
		if (!value.isNumber()) {
			errors.put(name, "invalid value");
			return 0.0;
		}
		double result = value.asDouble();
		if (result < 0.0) {
			errors.put(name, Messages.CANNOT_BE_NEGATIVE);
			return 0.0;
		}
		return result;
	}

	private static double readPositiveDouble(JsonObject requrst, String name, ValidationResult errors, double max) {
		JsonValue value = requrst.get(name);
		if (value == null) {
			errors.put(name, Messages.CANNOT_BE_EMPTY);
			return 0.0;
		}
		if (!value.isNumber()) {
			errors.put(name, "invalid value");
			return 0.0;
		}
		double result = value.asDouble();
		if (result < 0.0) {
			errors.put(name, Messages.CANNOT_BE_NEGATIVE);
			return 0.0;
		}
		if (result > max) {
			errors.put(name, "Cannot be more than " + max);
			return 0.0;
		}
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/config/save";
	}

}
