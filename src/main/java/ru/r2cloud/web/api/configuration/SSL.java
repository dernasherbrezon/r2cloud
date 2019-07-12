package ru.r2cloud.web.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;

public class SSL extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(SSL.class);

	private final Configuration config;
	private final AcmeClient acmeClient;

	public SSL(Configuration config, AcmeClient acmeClient) {
		this.config = config;
		this.acmeClient = acmeClient;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = new JsonObject();
		if (acmeClient.getSslDomain() != null) {
			entity.add("domain", acmeClient.getSslDomain());
		}
		entity.add("enabled", acmeClient.isSSLEnabled());
		entity.add("running", acmeClient.isRunning());
		entity.add("agreeWithToC", acmeClient.isSSLEnabled() || acmeClient.isRunning());
		JsonArray messages = new JsonArray();
		for (String cur : acmeClient.getMessages()) {
			JsonObject curObject = new JsonObject();
			curObject.add("message", cur);
			messages.add(curObject);
		}
		entity.add("log", messages);
		result.setData(entity.toString());
		return result;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		boolean sslEnabled = WebServer.getBoolean(request, "enabled");
		boolean agreeWithToC = WebServer.getBoolean(request, "agreeWithToC");
		String domain = WebServer.getString(request, "domain");

		ValidationResult errors = new ValidationResult();
		if (sslEnabled && !agreeWithToC) {
			errors.put("agreeWithToC", "You must agree with ToC");
		}
		if (domain == null || domain.trim().length() == 0) {
			errors.put("domain", "Cannot be empty");
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}

		if (sslEnabled && !acmeClient.isSSLEnabled()) {
			config.setProperty("acme.ssl.domain", domain);
			config.update();
			acmeClient.setup();
		}

		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/ssl";
	}
}