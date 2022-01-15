package ru.r2cloud.sdr;

import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.Util;

public class SdrStatusDao implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(SdrStatusDao.class);

	private final Configuration config;
	private final Metrics metrics;

	private ScheduledExecutorService executor;
	private SdrStatus status;
	private SdrStatusProcess statusProcess;

	public SdrStatusDao(Configuration config, Metrics metrics, ProcessFactory processFactory) {
		this.config = config;
		this.metrics = metrics;
		switch (config.getSdrType()) {
		case RTLSDR:
			statusProcess = new RtlStatusProcess(config, processFactory);
			break;
		case PLUTOSDR:
			statusProcess = new PlutoStatusProcess(config, processFactory);
			break;
		case SDRSERVER:
			statusProcess = new SdrStatusProcess() {

				@Override
				public void terminate(long timeout) {
					// do nothing
				}

				@Override
				public SdrStatus getStatus() {
					// sdr-server doesn't support health checks yet
					SdrStatus result = new SdrStatus();
					result.setDongleConnected(true);
					return result;
				}
			};
			break;
		default:
			throw new IllegalArgumentException("unsupported sdr type: " + config.getSdrType());
		}
	}

	@Override
	public synchronized void start() {
		metrics.getHealthRegistry().register("rtltest", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				return ResultUtil.healthy();
			}
		});
		metrics.getHealthRegistry().register("rtldongle", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				SdrStatus curStatus = statusProcess.getStatus();
				if (curStatus == null) {
					return ResultUtil.unknown();
				}
				synchronized (SdrStatusDao.this) {
					status = curStatus;
					if (status.isDongleConnected()) {
						return ResultUtil.healthy();
					} else {
						return ResultUtil.unhealthy(status.getError());
					}
				}
			}
		});

		LOG.info("started");
	}

	@Override
	public synchronized void stop() {
		statusProcess.terminate(1000);
		Util.shutdown(executor, config.getThreadPoolShutdownMillis());
		executor = null;
		LOG.info("stopped");
	}

}
