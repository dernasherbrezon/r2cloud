package ru.r2cloud.model;

import java.util.List;

import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class SSLStatus {

	private boolean sslEnabled;
	private boolean sslRunning;
	private boolean agreeWithToC;
	private List<String> messages;

	public boolean isAgreeWithToC() {
		return agreeWithToC;
	}

	public void setAgreeWithToC(boolean agreeWithToC) {
		this.agreeWithToC = agreeWithToC;
	}

	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	public boolean isSslRunning() {
		return sslRunning;
	}

	public void setSslRunning(boolean sslRunning) {
		this.sslRunning = sslRunning;
	}

	public List<String> getMessages() {
		return messages;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

	public static SSLStatus fromSession(IHTTPSession session) {
		SSLStatus result = new SSLStatus();
		result.setSslEnabled(WebServer.getBoolean(session, "sslEnabled"));
		result.setAgreeWithToC(WebServer.getBoolean(session, "agreeWithToC"));
		return result;
	}

	public ValidationResult validate() {
		ValidationResult result = new ValidationResult();
		if (sslEnabled && !agreeWithToC) {
			result.put("agreeWithToC", "You must agree with ToC");
		}
		return result;
	}
}
