package ru.r2cloud.web.api.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.AutoUpdate;
import ru.r2cloud.model.GeneralConfiguration;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.api.Messages;

public class General extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(General.class);

	private final Configuration config;
	private final AutoUpdate autoUpdate;

	public General(Configuration config, AutoUpdate autoUpdate) {
		this.config = config;
		this.autoUpdate = autoUpdate;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonObject entity = config.getGeneralConfiguration().toJson();
		entity.add("autoUpdate", autoUpdate.isEnabled());
		result.setData(entity.toString());
		return result;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		ValidationResult errors = new ValidationResult();
		GeneralConfiguration generalConfig = GeneralConfiguration.fromJson(request);
		if (!generalConfig.isLocationAuto()) {
			if (generalConfig.getLat() == null) {
				errors.put("lat", Messages.CANNOT_BE_EMPTY);
			}
			if (generalConfig.getLng() == null) {
				errors.put("lng", Messages.CANNOT_BE_EMPTY);
			}
		}
		if (!errors.isEmpty()) {
			LOG.info("unable to save: {}", errors);
			return new BadRequest(errors);
		}
		autoUpdate.setEnabled(generalConfig.isAutoUpdate());
		config.saveGeneralConfiguration(generalConfig);
		config.update();
		return new Success();
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/general";
	}
}
