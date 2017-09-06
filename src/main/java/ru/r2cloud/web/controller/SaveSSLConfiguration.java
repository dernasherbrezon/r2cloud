package ru.r2cloud.web.controller;

import java.util.logging.Logger;

import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.model.ConfigurationBean;
import ru.r2cloud.model.SSLStatus;
import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Redirect;
import ru.r2cloud.web.ValidationResult;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class SaveSSLConfiguration extends AbstractHttpController {

	private final static Logger LOG = Logger.getLogger(SaveSSLConfiguration.class.getName());

	private final Configuration props;
	private final AcmeClient acmeClient;

	public SaveSSLConfiguration(Configuration props, AcmeClient acmeClient) {
		this.props = props;
		this.acmeClient = acmeClient;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		SSLStatus bean = SSLStatus.fromSession(session);
		ValidationResult errors = bean.validate();
		// FIXME check if ddns configured
		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			ModelAndView result = new ModelAndView("config");
			result.put("errors", errors);
			result.put("entity", ConfigurationBean.fromConfig(props));
			result.put("ddnstypes", DDNSType.values());
			result.put("ddnsEntity", bean);
			result.put("activeTab", "ssl");
			return result;
		}

		if (bean.isSslEnabled() && !acmeClient.isSSLEnabled()) {
			acmeClient.setup();
		}

		return new Redirect("/admin/config");
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/config/ssl/save";
	}
}