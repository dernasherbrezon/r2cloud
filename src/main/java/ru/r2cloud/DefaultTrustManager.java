package ru.r2cloud;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class DefaultTrustManager implements X509TrustManager {

	@Override
	public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		//do nothing
	}

	@Override
	public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		//do nothing
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		//do nothing
		return null;
	}

}
