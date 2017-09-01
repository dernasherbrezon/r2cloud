package ru.r2cloud.model;

import ru.r2cloud.ddns.DDNSType;
import ru.r2cloud.uitl.Configuration;

public class DDNSBean {

	private DDNSType type;
	private String noipUsername;
	private String noipPassword;
	private String noipDomain;
	
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
		entity.setType(DDNSType.valueOf(props.getProperty("ddns.type")));
		return entity;
	}
}
