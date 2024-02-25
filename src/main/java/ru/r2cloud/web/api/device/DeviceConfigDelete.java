package ru.r2cloud.web.api.device;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;

public class DeviceConfigDelete extends AbstractHttpController {

	@Override
	public ModelAndView doPost(JsonObject request) {
		System.out.println(request.get("ids"));
		// TODO Auto-generated method stub
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/device/config/delete";
	}
}
