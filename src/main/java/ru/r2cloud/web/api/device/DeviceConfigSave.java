package ru.r2cloud.web.api.device;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.device.Device;
import ru.r2cloud.device.DeviceManager;
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
import ru.r2cloud.web.Success;
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
		config.setMinimumFrequency((long) (readPositiveDouble(request, "minimumFrequency", errors) * 1000_000));
		config.setMaximumFrequency((long) (readPositiveDouble(request, "maximumFrequency", errors) * 1000_000));
		if (config.getMinimumFrequency() > config.getMaximumFrequency()) {
			errors.put("minimumFrequency", "Cannot be more than maximum frequency");
		}
		switch (deviceType) {
		case RTLSDR: {
			config.setRtlDeviceId((int) readOptionalLong(request, "rtlDeviceId", 0, errors));
			if (config.getRtlDeviceId() < 0) {
				errors.put("rtlDeviceId", Messages.CANNOT_BE_NEGATIVE);
			}
			config.setGain((float) readPositiveDouble(request, "gain", errors));
			if (config.getGain() > 49.6f) {
				errors.put("gain", "Cannot be more than 49.6");
			}
			config.setBiast(WebServer.getBoolean(request, "biast"));
			config.setPpm((int) readOptionalLong(request, "ppm", 0, errors));
			break;
		}
		case LORAAT: {
			// this is not a hostname, but rather device location
			config.setHost(WebServer.getString(request, "host"));
			if (config.getHost() == null) {
				errors.put("host", Messages.CANNOT_BE_EMPTY);
			}
			config.setGain(readLong(request, "gain", errors));
			if (config.getGain() > 6) {
				errors.put("gain", "Cannot be more than 6");
			}
			break;
		}
		case LORAATBLE: {
			// this is not a hostname, but rather ble address
			config.setHost(WebServer.getString(request, "host"));
			if (config.getHost() == null) {
				errors.put("host", Messages.CANNOT_BE_EMPTY);
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
			config.setGain((float) readPositiveDouble(request, "gain", errors));
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
			config.setGain((float) readPositiveDouble(request, "gain", errors));
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

		switch (deviceType) {
		case RTLSDR: {
			props.saveRtlSdrConfiguration(config);
			break;
		}
		case LORAAT: {
			props.saveLoraAtConfiguration(config);
			break;
		}
		case LORAATBLE: {
			props.saveLoraAtBleConfiguration(config);
			break;
		}
		case LORAATWIFI: {
			props.saveLoraAtWifiConfiguration(config);
			break;
		}
		case PLUTOSDR: {
			props.savePlutoSdrConfiguration(config);
			break;
		}
		case SDRSERVER: {
			props.saveSdrServerConfiguration(config);
			break;
		}
		case SPYSERVER: {
			props.saveSpyServerConfiguration(config);
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + deviceType);
		}
		props.update();
		return new Success();
	}

	private static SdrServerConfiguration readSdrServerConfiguration(JsonObject request, ValidationResult errors) {
		SdrServerConfiguration result = new SdrServerConfiguration();
		result.setBandwidth((long) (readPositiveDouble(request, "bandwidth", errors) * 1000_000));
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
		result.setTolerance(readPositiveDouble(request, "rotatorTolerance", errors));
		if (result.getTolerance() > 360.0f) {
			errors.put("rotatorTolerance", "cannot be more than 360 degrees");
		}
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
			result.setMinElevation(readPositiveDouble(request, "minElevation", errors));
			if (result.getMinElevation() > 90.0) {
				errors.put("minElevation", "cannot be more than 90 degrees");
			}
			result.setGuaranteedElevation(readPositiveDouble(request, "guaranteedElevation", errors));
			if (result.getGuaranteedElevation() > 90.0) {
				errors.put("guaranteedElevation", "cannot be more than 90 degrees");
			}
			if (result.getMinElevation() > result.getGuaranteedElevation()) {
				errors.put("minElevation", "Cannot be more than guaranteed elevation");
			}
			break;
		}
		case FIXED_DIRECTIONAL: {
			result.setAzimuth(readPositiveDouble(request, "azimuth", errors));
			if (result.getAzimuth() > 360.0f) {
				errors.put("azimuth", "cannot be more than 360 degrees");
			}
			result.setElevation(readPositiveDouble(request, "elevation", errors));
			if (result.getElevation() > 90.0) {
				errors.put("elevation", "cannot be more than 90 degrees");
			}
			result.setBeamwidth(readPositiveDouble(request, "beamwidth", errors));
			if (result.getBeamwidth() > 360.0f) {
				errors.put("beamwidth", "cannot be more than 360 degrees");
			}
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

	private static double readPositiveDouble(JsonObject requrst, String name, ValidationResult errors) {
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
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/config/save";
	}

}
