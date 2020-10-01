package ru.r2cloud.web.api.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.satellite.Scheduler;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ScheduleComplete extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduleComplete.class);

	private final Scheduler scheduler;

	public ScheduleComplete(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	@Override
	public ModelAndView doPost(JsonObject request) {
		String observationId = WebServer.getString(request, "id");
		ValidationResult errors = new ValidationResult();
		if (observationId == null || observationId.trim().length() == 0) {
			errors.put("id", Messages.CANNOT_BE_EMPTY);
		}

		if (!errors.isEmpty()) {
			LOG.info("unable to complete: {}", errors);
			return new BadRequest(errors);
		}

		if (scheduler.completeImmediately(observationId)) {
			return new Success();
		} else {
			return new NotFound();
		}
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/schedule/immediately/complete";
	}

}
