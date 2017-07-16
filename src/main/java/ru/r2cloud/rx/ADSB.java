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

public class ADSB {

	private static final Logger LOG = Logger.getLogger(ADSB.class.getName());

	private final Properties props;
	private final ADSBDao dao;

	private Socket socket;
	private boolean started = true;
	private Thread thread;
	private long throttleIntervalMillis;

	public ADSB(Properties props, ADSBDao dao) {
		this.props = props;
		this.dao = dao;
		throttleIntervalMillis = Long.valueOf(props.getProperty("rx.adsb.reconnect.interval"));
	}

	public synchronized void start() {
		LOG.info("starting..");
		started = true;
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
						LOG.log(Level.SEVERE, "unable to connect to the dump1090: " + host + ":" + port, e);
						throttle();
						continue;
					}
					BufferedReader in;
					try {
						in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					} catch (IOException e) {
						LOG.log(Level.SEVERE, "cannot get input stream", e);
						throttle();
						continue;
					}
					String curLine = null;
					try {
						LOG.info("started");
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
						}
					} catch (IOException e) {
						//do not log error. socket closed
						if (!socket.isClosed()) {
							LOG.log(Level.SEVERE, "unable to read data", e);
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

	private void throttle() {
		try {
			Thread.sleep(throttleIntervalMillis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public synchronized void stop() {
		started = false;
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
