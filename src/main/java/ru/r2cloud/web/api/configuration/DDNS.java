package ru.r2cloud.web.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

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
		entity.add("username", config.getProperty(USERNAME_PROPERTY_NAME));
		entity.add("password", config.getProperty(PASSWORD_PROPERTY_NAME));
		entity.add("domain", config.getProperty(DOMAIN_PROPERTY_NAME));
		entity.add("type", config.getDdnsType(TYPE_PROPERTY_NAME).toString());
		entity.add("currentIp", config.getProperty("ddns.ip"));
		result.setData(entity.toString());
		return result;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}

		DDNSType type = DDNSType.valueOf(((JsonObject) request).getString("type", null));
		String username = ((JsonObject) request).getString("username", null);
		String password = ((JsonObject) request).getString("password", null);
		String domain = ((JsonObject) request).getString("domain", null);

		ValidationResult errors = new ValidationResult();
		switch (type) {
		case NOIP:
			if (username == null || username.trim().length() == 0) {
				errors.put("username", Messages.CANNOT_BE_EMPTY);
			}
			if (password == null || password.trim().length() == 0) {
				errors.put("password", Messages.CANNOT_BE_EMPTY);
			}
			if (domain == null || domain.trim().length() == 0) {
				errors.put("domain", Messages.CANNOT_BE_EMPTY);
			}
			break;
		default:
			break;
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}

		config.setProperty(USERNAME_PROPERTY_NAME, username);
		config.setProperty(PASSWORD_PROPERTY_NAME, password);
		config.setProperty(DOMAIN_PROPERTY_NAME, domain);
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