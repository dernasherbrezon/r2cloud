package ru.r2cloud.util;

import ru.r2cloud.metrics.Status;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheck.ResultBuilder;

public final class ResultUtil {

	public static Result unhealthy(String message) {
		ResultBuilder builder = Result.builder();
		builder.unhealthy().withMessage(message).withDetail("status", Status.ERROR);
		return builder.build();
	}
	
	public static Result healthy() {
		ResultBuilder builder = Result.builder();
		builder.healthy().withDetail("status", Status.SUCCESS);
		return builder.build();
	}
	
	public static Result unknown() {
		ResultBuilder builder = Result.builder();
		builder.healthy().withDetail("status", Status.UNKNOWN);
		return builder.build();
	}

	private ResultUtil() {
		//do nothing
	}
}
