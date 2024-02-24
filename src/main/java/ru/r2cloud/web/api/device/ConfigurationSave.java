package ru.r2cloud.web.api.device;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ConfigurationSave extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ConfigurationSave.class);
	private final Configuration config;

	public ConfigurationSave(Configuration config) {
		this.config = config;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		ValidationResult errors = new ValidationResult();
		DeviceType deviceType = readDeviceType(request, errors);
		// fail fast fist to reduce complexity of the code below.
		// no need to check if device type is not null
		if (deviceType == null) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}
		DeviceConfiguration config = new DeviceConfiguration();
		config.setId(request.getString("id", null));
		config.setMinimumFrequency((long) (readPositiveFloat(request, "minimumFrequency", errors) * 1000_000));
		config.setMaximumFrequency((long) (readPositiveFloat(request, "maximumFrequency", errors) * 1000_000));
		switch (deviceType) {
		case RTLSDR: {
			config.setRtlDeviceId((int) readOptionalLong(request, "rtlDeviceId", 0, errors));
			if (config.getRtlDeviceId() < 0) {
				errors.put("rtlDeviceId", Messages.CANNOT_BE_NEGATIVE);
			}
			config.setGain(readPositiveFloat(request, "gain", errors));
			if (config.getGain() > 49.6f) {
				errors.put("gain", "Cannot be more than 49.6");
			}
			config.setBiast(WebServer.getBoolean(request, "biast"));
			config.setPpm((int) readOptionalLong(request, "ppm", 0, errors));
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
		// TODO Auto-generated method stub
		return super.doPost(request);
	}

	private static RotatorConfiguration readRotatorConfiguration(JsonObject request, ValidationResult errors) {
		RotatorConfiguration result = new RotatorConfiguration();
		result.setHostname(WebServer.getString(request, "rotctrldHostname"));
		if (result.getHostname() == null) {
			errors.put("rotctrldHostname", Messages.CANNOT_BE_EMPTY);
		} else {
			try {
				InetAddress.getByName(result.getHostname());
			} catch (Exception e) {
				errors.put("rotctrldHostname", "invalid hostname");
			}
		}
		result.setPort((int) readLong(request, "rotctrldPort", errors));
		result.setTolerance(readPositiveFloat(request, "rotatorTolerance", errors));
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
			result.setMinElevation(readPositiveFloat(request, "minElevation", errors));
			if (result.getMinElevation() > 90.0) {
				errors.put("minElevation", "cannot be more than 90 degrees");
			}
			result.setGuaranteedElevation(readPositiveFloat(request, "guaranteedElevation", errors));
			if (result.getGuaranteedElevation() > 90.0) {
				errors.put("guaranteedElevation", "cannot be more than 90 degrees");
			}
			break;
		}
		case FIXED_DIRECTIONAL: {
			result.setAzimuth(readPositiveFloat(request, "azimuth", errors));
			if (result.getAzimuth() > 360.0f) {
				errors.put("azimuth", "cannot be more than 360 degrees");
			}
			result.setElevation(readPositiveFloat(request, "elevation", errors));
			if (result.getElevation() > 90.0) {
				errors.put("elevation", "cannot be more than 90 degrees");
			}
			result.setBeamwidth(readPositiveFloat(request, "beamwidth", errors));
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

	private static float readPositiveFloat(JsonObject requrst, String name, ValidationResult errors) {
		JsonValue value = requrst.get(name);
		if (value == null) {
			errors.put(name, Messages.CANNOT_BE_EMPTY);
			return 0.0f;
		}
		if (!value.isNumber()) {
			errors.put(name, "invalid value");
			return 0.0f;
		}
		float result = value.asFloat();
		if (result < 0.0f) {
			errors.put(name, Messages.CANNOT_BE_NEGATIVE);
			return 0.0f;
		}
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/config/save";
	}

}
