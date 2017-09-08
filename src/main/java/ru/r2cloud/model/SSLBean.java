package ru.r2cloud.model;

import java.util.List;

import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class SSLBean {

	private String sslDomain;
	private boolean sslEnabled;
	private boolean sslRunning;
	private boolean agreeWithToC;
	private List<String> messages;

	public String getSslDomain() {
		return sslDomain;
	}

	public void setSslDomain(String sslDomain) {
		this.sslDomain = sslDomain;
	}

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

	public static SSLBean fromSession(IHTTPSession session) {
		SSLBean result = new SSLBean();
		result.setSslEnabled(WebServer.getBoolean(session, "sslEnabled"));
		result.setAgreeWithToC(WebServer.getBoolean(session, "agreeWithToC"));
		result.setSslDomain(WebServer.getParameter(session, "sslDomain"));
		return result;
	}

	public static SSLBean fromAcmeClient(AcmeClient acmeClient) {
		SSLBean result = new SSLBean();
		result.setMessages(acmeClient.getMessages());
		result.setSslEnabled(acmeClient.isSSLEnabled());
		result.setSslRunning(acmeClient.isRunning());
		result.setAgreeWithToC(result.isSslEnabled() || result.isSslRunning());
		result.setSslDomain(acmeClient.getSslDomain());
		return result;
	}

	public ValidationResult validate() {
		ValidationResult result = new ValidationResult();
		if (sslEnabled && !agreeWithToC) {
			result.put("agreeWithToC", "You must agree with ToC");
		}
		if (sslDomain == null || sslDomain.trim().length() == 0) {
			result.put("sslDomain", "Cannot be empty");
		}
		return result;
	}
}
