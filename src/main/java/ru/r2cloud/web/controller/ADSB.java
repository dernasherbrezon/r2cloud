package ru.r2cloud.web.controller;

import ru.r2cloud.model.ConfigurationBean;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class ADSB extends AbstractHttpController {
	
	private final Configuration props;

	public ADSB(Configuration props) {
		this.props = props;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView("adsb");
		result.put("entity", ConfigurationBean.fromConfig(props));
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/adsb";
	}

}
