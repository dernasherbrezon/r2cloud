package ru.r2cloud.rx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.ModeSReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.Lifecycle;
import ru.r2cloud.RtlSdrLock;
import ru.r2cloud.metrics.FormattedCounter;
import ru.r2cloud.metrics.MetricFormat;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ResultUtil;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.Util;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry.MetricSupplier;
import com.codahale.metrics.health.HealthCheck;

public class ADSB implements Lifecycle {

	private static final Logger LOG = LoggerFactory.getLogger(ADSB.class);

	private final Configuration props;
	private final ADSBDao dao;
	private final boolean enabled;
	private final long throttleIntervalMillis;
	private final RtlSdrLock lock;

	private Socket socket;
	private volatile boolean started = false;
	private volatile String connectionError = "Unknown status";
	private Counter counter;
	private Thread thread;
	private Process dump1090;

	public ADSB(Configuration props, ADSBDao dao, RtlSdrLock lock) {
		this.props = props;
		this.dao = dao;
		this.lock = lock;
		this.enabled = props.getBoolean("rx.adsb.enabled");
		this.throttleIntervalMillis = props.getLong("rx.adsb.reconnect.interval");
	}

	@Override
	public synchronized void start() {
		if (!enabled || started) {
			return;
		}

		if (!lock.tryLock(this)) {
			LOG.info("unable to acquire lock");
			return;
		}

		LOG.info("starting..");
		started = true;

		Metrics.HEALTH_REGISTRY.register("adsb", new HealthCheck() {

			@Override
			protected Result check() throws Exception {
				if (!started) {
					return ResultUtil.unknown();
				} else if (connectionError == null) {
					return ResultUtil.healthy();
				} else {
					return ResultUtil.unhealthy(connectionError);
				}
			}
		});
		this.counter = Metrics.REGISTRY.counter("adsb", new MetricSupplier<Counter>() {
			@Override
			public Counter newMetric() {
				return new FormattedCounter(MetricFormat.NORMAL);
			}

		});

		thread = new Thread(new SafeRunnable() {

			@Override
			public void doRun() {
				// give some time for dump1090 to initialize - thus throttle
				throttle();

				while (!Thread.currentThread().isInterrupted() && started) {
					String host = props.getProperty("rx.adsb.hostname");
					int port = props.getInteger("rx.adsb.port");
					try {
						socket = new Socket(host, port);
						socket.setKeepAlive(true);
						socket.setSoTimeout(0);
					} catch (Exception e) {
						connectionError = "unable to connect to the dump1090: " + host + ":" + port;
						LOG.error(connectionError + ". check stdout log", e);
						throttle();
						continue;
					}
					BufferedReader in;
					try {
						in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					} catch (IOException e) {
						connectionError = "cannot get input stream";
						LOG.error(connectionError, e);
						throttle();
						continue;
					}
					connectionError = null;
					String curLine = null;
					try {
						LOG.info("listening for adsb data from " + host + ":" + port);
						while ((curLine = in.readLine()) != null) {
							String messageStr = curLine.substring(1, curLine.length() - 1);
							if (messageStr.equals("0000")) {
								// sometimes dump1090 returns this message
								continue;
							}
							try {
								ModeSReply message = Decoder.genericDecoder(messageStr);
								dao.save(message);
							} catch (Exception e) {
								LOG.info("unknown message received: " + curLine, e);
							}
							counter.inc();
						}
					} catch (IOException e) {
						// do not log error. socket closed
						if (!socket.isClosed()) {
							LOG.error("unable to read data", e);
						} else {
							connectionError = "connection was closed remotely: " + e.getMessage();
						}
						closeSocket();
						throttle();
						continue;
					}
				}
			}
		}, "adsb-reader");
		thread.start();
	}

	private void startDump1090() {
		try {
			Integer ppm = props.getInteger("ppm.current");
			if (ppm == null) {
				ppm = 0;
			}
			dump1090 = new ProcessBuilder().inheritIO().command(new String[] { props.getProperty("rx.adsb.dump1090"), "--raw", "--net", "--quiet", "--ppm", String.valueOf(ppm) }).start();
			LOG.info("dump1090 started..");
		} catch (IOException e1) {
			LOG.error("unable to start dump1090", e1);
		}
	}

	private void throttle() {
		if (dump1090 == null || !dump1090.isAlive()) {
			LOG.info("dump1090 is not alive. starting it");
			startDump1090();
		}
		try {
			Thread.sleep(throttleIntervalMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public synchronized void stop() {
		if (!enabled || !started) {
			return;
		}
		started = false;
		closeSocket();
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join(props.getLong("rx.adsb.timeout"));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				LOG.error("unable to stop thread", e);
			}
		}
		Util.shutdown("dump1090", dump1090, throttleIntervalMillis);
		LOG.info("stopped");

		//never call unlock. it should be always subscribed
//		lock.unlock(this);
	}

	private void closeSocket() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				LOG.info("unable to close socket", e);
			}
		}
	}

}
