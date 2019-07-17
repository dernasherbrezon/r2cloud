package ru.r2cloud.ddns.noip;

public class RetryException extends Exception {
	
	private static final long serialVersionUID = -7456809427495492302L;
	
	private final long retryTimeout;

	public RetryException(long retryTimeout) {
		this.retryTimeout = retryTimeout;
	}
	
	public long getRetryTimeout() {
		return retryTimeout;
	}
}
