package ru.r2cloud.model;

import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

@Deprecated
public class DDNSBean {

	private DDNSType type;
	private String noipUsername;
	private String noipPassword;
	private String noipDomain;
	private String currentIp;
	
	public String getCurrentIp() {
		return currentIp;
	}
	
	public void setCurrentIp(String currentIp) {
		this.currentIp = currentIp;
	}

	public DDNSType getType() {
		return type;
	}

	public void setType(DDNSType type) {
		this.type = type;
	}

	public String getNoipUsername() {
		return noipUsername;
	}

	public void setNoipUsername(String noipUsername) {
		this.noipUsername = noipUsername;
	}

	public String getNoipPassword() {
		return noipPassword;
	}

	public void setNoipPassword(String noipPassword) {
		this.noipPassword = noipPassword;
	}

	public String getNoipDomain() {
		return noipDomain;
	}

	public void setNoipDomain(String noipDomain) {
		this.noipDomain = noipDomain;
	}

	public static DDNSBean fromConfig(Configuration props) {
		DDNSBean entity = new DDNSBean();
		entity.setNoipUsername(props.getProperty("ddns.noip.username"));
		entity.setNoipPassword(props.getProperty("ddns.noip.password"));
		entity.setNoipDomain(props.getProperty("ddns.noip.domain"));
		entity.setType(props.getDdnsType("ddns.type"));
		entity.setCurrentIp(props.getProperty("ddns.ip"));
		return entity;
	}

	public void toConfig(Configuration props) {
		props.setProperty("ddns.noip.username", noipUsername);
		props.setProperty("ddns.noip.password", noipPassword);
		props.setProperty("ddns.noip.domain", noipDomain);
		props.setProperty("ddns.type", type.name());
	}

	public static DDNSBean fromSession(IHTTPSession session) {
		DDNSBean result = new DDNSBean();
		result.setType(DDNSType.valueOf(WebServer.getParameter(session, "type")));
		result.setNoipUsername(WebServer.getParameter(session, "noipUsername"));
		result.setNoipPassword(WebServer.getParameter(session, "noipPassword"));
		result.setNoipDomain(WebServer.getParameter(session, "noipDomain"));
		return result;
	}

	public ValidationResult validate() {
		ValidationResult errors = new ValidationResult();
		switch (type) {
		case NOIP:
			if (noipUsername == null || noipUsername.trim().length() == 0) {
				errors.put("noipUsername", "Cannot be empty");
			}
			if (noipPassword == null || noipPassword.trim().length() == 0) {
				errors.put("noipPassword", "Cannot be empty");
			}
			if (noipDomain == null || noipDomain.trim().length() == 0) {
				errors.put("noipDomain", "Cannot be empty");
			}
			break;
		default:
			break;
		}
		return errors;
	}
}
