package ru.r2cloud.web.controller;

import ru.r2cloud.model.ConfigurationBean;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Configuration extends AbstractHttpController {

	private final ru.r2cloud.uitl.Configuration props;

	public Configuration(ru.r2cloud.uitl.Configuration props) {
		this.props = props;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView("config");
		result.put("entity", ConfigurationBean.fromConfig(props));
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/config";
	}

}
