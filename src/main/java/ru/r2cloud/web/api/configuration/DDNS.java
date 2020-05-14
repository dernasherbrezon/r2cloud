package ru.r2cloud.web.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class DDNS extends AbstractHttpController {

	private static final String TYPE_PROPERTY_NAME = "ddns.type";
	private static final String DOMAIN_PROPERTY_NAME = "ddns.noip.domain";
	private static final String PASSWORD_PROPERTY_NAME = "ddns.noip.password";
	private static final String USERNAME_PROPERTY_NAME = "ddns.noip.username";
	
	private static final String PASSWORD_PARAMETER = "password";
	private static final String USERNAME_PARAMETER = "username";
	private static final String DOMAIN_PARAMETER = "domain";
	
	private static final Logger LOG = LoggerFactory.getLogger(DDNS.class);

	private final Configuration config;
	private final DDNSClient ddnsClient;

	public DDNS(Configuration config, DDNSClient ddnsClient) {
		this.config = config;
		this.ddnsClient = ddnsClient;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = new JsonObject();
		String username = config.getProperty(USERNAME_PROPERTY_NAME);
		if (username != null) {
			entity.add(USERNAME_PARAMETER, username);
		}
		String password = config.getProperty(PASSWORD_PROPERTY_NAME);
		if (password != null) {
			entity.add(PASSWORD_PARAMETER, password);
		}
		String domain = config.getProperty(DOMAIN_PROPERTY_NAME);
		if (domain != null) {
			entity.add(DOMAIN_PARAMETER, domain);
		}
		entity.add("type", config.getDdnsType(TYPE_PROPERTY_NAME).toString());
		String currentIp = config.getProperty("ddns.ip");
		if (currentIp != null) {
			entity.add("currentIp", currentIp);
		}
		result.setData(entity.toString());
		return result;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		String typeStr = WebServer.getString(request, "type");
		String username = request.getString(USERNAME_PARAMETER, null);
		String password = request.getString(PASSWORD_PARAMETER, null);
		String domain = request.getString(DOMAIN_PARAMETER, null);

		ValidationResult errors = new ValidationResult();
		if (typeStr == null) {
			errors.put("type", Messages.CANNOT_BE_EMPTY);
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}

		DDNSType type;
		try {
			type = DDNSType.valueOf(typeStr);
		} catch (Exception e) {
			LOG.info("unknown ddns type: {}", typeStr, e);
			errors.put("type", "unknown type");
			return new BadRequest(errors);
		}

		switch (type) {
		case NOIP:
			if (username == null || username.trim().length() == 0) {
				errors.put(USERNAME_PARAMETER, Messages.CANNOT_BE_EMPTY);
			}
			if (password == null || password.trim().length() == 0) {
				errors.put(PASSWORD_PARAMETER, Messages.CANNOT_BE_EMPTY);
			}
			if (domain == null || domain.trim().length() == 0) {
				errors.put(DOMAIN_PARAMETER, Messages.CANNOT_BE_EMPTY);
			}
			break;
		default:
			break;
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}

		if (username != null) {
			config.setProperty(USERNAME_PROPERTY_NAME, username);
		}
		if (password != null) {
			config.setProperty(PASSWORD_PROPERTY_NAME, password);
		}
		if (domain != null) {
			config.setProperty(DOMAIN_PROPERTY_NAME, domain);
		}
		config.setProperty(TYPE_PROPERTY_NAME, type.name());
		config.update();

		ddnsClient.stop();
		ddnsClient.start();

		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/ddns";
	}
}