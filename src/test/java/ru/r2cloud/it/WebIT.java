package ru.r2cloud.it;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@SuiteClasses({ LoginPageIT.class })
public class WebIT {

	private static final Logger LOG = LoggerFactory.getLogger(WebIT.class);

	@BeforeClass
	public static void start() {
		assertStarted();
	}

	static void assertStarted() {
		int currentRetry = 0;
		int maxRetries = 5;
		while (currentRetry < maxRetries) {
			if (isLoaded()) {
				break;
			}
			currentRetry++;
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private static boolean isLoaded() {
		HttpURLConnection con = null;
		try {
			disableSslVerification();
			URL obj = new URL("https://localhost");
			con = (HttpURLConnection) obj.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			boolean result = con.getResponseCode() == 200;
			if (result) {
				LOG.info("loaded");
			} else {
				LOG.info("not loaded: " + con.getResponseCode());
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			LOG.info("not loaded: " + e.getMessage());
			return false;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	private static void disableSslVerification() {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
					// do nothing
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
					// do nothing
				}
			} };

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}

}
