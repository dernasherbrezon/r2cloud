package ru.r2cloud.web.controller;

import java.util.logging.Logger;

import ru.r2cloud.AutoUpdate;
import ru.r2cloud.ddns.DDNSClient;
import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.model.ConfigurationBean;
import ru.r2cloud.model.DDNSBean;
import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Redirect;
import ru.r2cloud.web.ValidationResult;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class SaveDDNSConfiguration extends AbstractHttpController {

	private final static Logger LOG = Logger.getLogger(SaveDDNSConfiguration.class.getName());

	private final Configuration props;
	private final DDNSClient client;
	private final AutoUpdate autoUpdate;
	
	public SaveDDNSConfiguration(Configuration props, DDNSClient client, AutoUpdate autoUpdate) {
		this.props = props;
		this.client = client;
		this.autoUpdate = autoUpdate;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		DDNSBean bean = DDNSBean.fromSession(session);
		ValidationResult errors = bean.validate();
		if (!errors.isEmpty()) {
			LOG.info("unable to save: " + errors);
			ModelAndView result = new ModelAndView("config");
			result.put("errors", errors);
			result.put("entity", ConfigurationBean.fromConfig(props));
			result.put("autoUpdate", autoUpdate.isEnabled());
			result.put("ddnstypes", DDNSType.values());
			result.put("ddnsEntity", bean);
			result.put("activeTab", "ddns");
			return result;
		}
		
		bean.toConfig(props);
		props.update();
		client.stop();
		client.start();

		return new Redirect("/admin/config");
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/config/ddns/save";
	}
}