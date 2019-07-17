package ru.r2cloud.ddns.noip;

public class NoIpException extends Exception {

	private static final long serialVersionUID = -2202052437185248771L;

	public NoIpException(String message) {
		super(message);
	}

	public NoIpException(String message, Exception e) {
		super(message, e);
	}

}
