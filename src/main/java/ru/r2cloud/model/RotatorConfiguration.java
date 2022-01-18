package ru.r2cloud.model;

public class RotatorConfiguration {

	private String id;
	private String hostname;
	private int port;
	private int timeout;
	private double tolerance;
	private int cycleMillis;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	public int getCycleMillis() {
		return cycleMillis;
	}

	public void setCycleMillis(int cycleMillis) {
		this.cycleMillis = cycleMillis;
	}

}
