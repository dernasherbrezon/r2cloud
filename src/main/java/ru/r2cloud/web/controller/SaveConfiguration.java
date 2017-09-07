package ru.r2cloud.web.controller;

import java.util.logging.Logger;

import ru.r2cloud.AutoUpdate;
import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.model.ConfigurationBean;
import ru.r2cloud.model.DDNSBean;
import ru.r2cloud.model.SSLStatus;
import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Redirect;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class SaveConfiguration extends AbstractHttpController {

	private final static Logger LOG = Logger.getLogger(SaveConfiguration.class.getName());

	private final Configuration props;
	private final AutoUpdate autoUpdate;
	private final DDNSClient ddnsClient;
	private final AcmeClient acmeClient;

	public SaveConfiguration(Configuration props, AutoUpdate autoUpdate, DDNSClient client, AcmeClient acmeClient) {
		this.props = props;
		this.autoUpdate = autoUpdate;
		this.ddnsClient = client;
		this.acmeClient = acmeClient;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		String action = WebServer.getParameter(session, "action");
		ValidationResult errors = new ValidationResult();
		ConfigurationBean bean = null;
		DDNSBean ddnsBean = null;
		SSLStatus sslBean = null;
		if (action.equals("general")) {
			Double lat = WebServer.getDouble(session, "lat");
			Double lon = WebServer.getDouble(session, "lon");
			bean = new ConfigurationBean();
			if (lat == null) {
				errors.put("lat", "Cannot be empty");
			} else {
				bean.setLat(String.valueOf(lat));
			}
			if (lon == null) {
				errors.put("lon", "Cannot be empty");
			} else {
				bean.setLon(String.valueOf(lon));
			}
		} else if (action.equals("ddns")) {
			ddnsBean = DDNSBean.fromSession(session);
			errors = ddnsBean.validate();
		} else if (action.equals("ssl")) {
			sslBean = SSLStatus.fromSession(session);
			errors = sslBean.validate();
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			ModelAndView result = new ModelAndView("config");
			result.put("errors", errors);
			if (bean != null) {
				result.put("entity", bean);
			} else {
				result.put("entity", ConfigurationBean.fromConfig(props));
			}
			if (ddnsBean != null) {
				result.put("ddnsEntity", ddnsBean);
			} else {
				result.put("ddnsEntity", DDNSBean.fromConfig(props));
			}
			if (sslBean != null) {
				result.put("sslEntity", sslBean);
			} else {
				result.put("sslEntity", SSLStatus.fromAcmeClient(acmeClient));
			}
			result.put("autoUpdate", autoUpdate.isEnabled());
			result.put("ddnstypes", DDNSType.values());
			result.put("activeTab", action);
			return result;
		}

		if (bean != null) {
			autoUpdate.setEnabled(WebServer.getBoolean(session, "autoUpdate"));
			props.setProperty("locaiton.lat", bean.getLat());
			props.setProperty("locaiton.lon", bean.getLon());
			props.update();
		} else if (ddnsBean != null) {
			ddnsBean.toConfig(props);
			props.update();
			ddnsClient.stop();
			ddnsClient.start();
		} else if (sslBean != null) {
			if (sslBean.isSslEnabled() && !acmeClient.isSSLEnabled()) {
				acmeClient.setup();
			}
		}

		return new Redirect("/admin/config?tab=" + action);
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/config/save";
	}

}
