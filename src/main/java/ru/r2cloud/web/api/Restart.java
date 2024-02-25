package ru.r2cloud.web.api;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.R2Cloud;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class Restart extends AbstractHttpController {

	@Override
	public ModelAndView doPost(JsonObject request) {
		R2Cloud.restart();
		return super.doPost(request);
	}
	
	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/restart";
	}
}
