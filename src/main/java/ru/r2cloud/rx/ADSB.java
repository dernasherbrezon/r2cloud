package ru.r2cloud.rx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opensky.libadsb.Decoder;
import org.opensky.libadsb.msgs.ModeSReply;

import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.uitl.ResultUtil;

import com.codahale.metrics.Counter;
import com.codahale.metrics.health.HealthCheck;

public class ADSB {

	private static final Logger LOG = Logger.getLogger(ADSB.class.getName());

	private final Properties props;
	private final ADSBDao dao;

	private Socket socket;
	private volatile boolean started = false;
	private volatile String connectionError = "Unknown status";
	private final Counter counter;
	private Thread thread;
	private Process dump1090;
	private long throttleIntervalMillis;

	public ADSB(Properties props, ADSBDao dao) {
		this.props = props;
		this.dao = dao;
		throttleIntervalMillis = Long.valueOf(props.getProperty("rx.adsb.reconnect.interval"));
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
		this.counter = Metrics.REGISTRY.counter("adsb");
	}

	public synchronized void start() {
		LOG.info("starting..");
		started = true;
		// give some time for dump1090 to initialize - thus throttle
		throttle();

		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted() && started) {
					String host = props.getProperty("rx.adsb.hostname");
					int port = Integer.parseInt(props.getProperty("rx.adsb.port"));
					try {
						socket = new Socket(host, port);
						socket.setKeepAlive(true);
						socket.setSoTimeout(0);
					} catch (Exception e) {
						connectionError = "unable to connect to the dump1090: " + host + ":" + port;
						LOG.log(Level.SEVERE, connectionError + ". check stdout log", e);
						throttle();
						continue;
					}
					BufferedReader in;
					try {
						in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					} catch (IOException e) {
						connectionError = "cannot get input stream";
						LOG.log(Level.SEVERE, connectionError, e);
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
								LOG.log(Level.INFO, "unknown message received: " + curLine, e);
							}
							counter.inc();
						}
					} catch (IOException e) {
						// do not log error. socket closed
						if (!socket.isClosed()) {
							LOG.log(Level.SEVERE, "unable to read data", e);
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
			dump1090 = new ProcessBuilder().inheritIO().command(new String[] { props.getProperty("rx.adsb.dump1090"), "--raw", "--net", "--quiet" }).start();
			LOG.info("dump1090 started..");
		} catch (IOException e1) {
			LOG.log(Level.SEVERE, "unable to start dump1090", e1);
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

	public synchronized void stop() {
		started = false;
		if (dump1090 != null) {
			dump1090.destroyForcibly();
		}
		closeSocket();
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join(Long.valueOf(props.getProperty("rx.adsb.timeout")));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "unable to stop thread", e);
			}
		}
		LOG.info("stopped");
	}

	private void closeSocket() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				LOG.log(Level.INFO, "unable to close socket", e);
			}
		}
	}

}
