package ru.r2cloud.web.controller;

import ru.r2cloud.AutoUpdate;
import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.model.ConfigurationBean;
import ru.r2cloud.model.DDNSBean;
import ru.r2cloud.model.SSLStatus;
import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Configuration extends AbstractHttpController {

	private final ru.r2cloud.uitl.Configuration props;
	private final AutoUpdate autoUpdate;
	private final AcmeClient acmeClient;

	public Configuration(ru.r2cloud.uitl.Configuration props, AutoUpdate autoUpdate, AcmeClient acmeClient) {
		this.props = props;
		this.autoUpdate = autoUpdate;
		this.acmeClient = acmeClient;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		String tab = WebServer.getParameter(session, "tab");
		if (tab == null) {
			tab = "general";
		}
		ModelAndView result = new ModelAndView("config");
		result.put("entity", ConfigurationBean.fromConfig(props));
		result.put("autoUpdate", autoUpdate.isEnabled());
		result.put("ddnstypes", DDNSType.values());
		result.put("ddnsEntity", DDNSBean.fromConfig(props));
		result.put("sslEntity", SSLStatus.fromAcmeClient(acmeClient));
		result.put("activeTab", tab);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/config";
	}

}
