package ru.r2cloud.it;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class RestClient implements Closeable {

	private static final Logger LOG = LoggerFactory.getLogger(RestClient.class);

	private CloseableHttpClient httpclient;
	private final String baseUrl;

	public RestClient(String baseUrl) throws Exception {
		this.baseUrl = baseUrl;
		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		this.httpclient = HttpClients.custom().setUserAgent("r2cloud/0.1 info@r2cloud.ru").setSSLSocketFactory(sslsf).build();
	}

	public boolean healthy() {
		HttpGet m = new HttpGet(baseUrl + "/api/v1/health");
		HttpResponse response = null;
		try {
			response = httpclient.execute(m);
			int statusCode = response.getStatusLine().getStatusCode();
			boolean result = statusCode == HttpStatus.SC_OK;
			if (!result) {
				LOG.info("status code: " + statusCode);
			}
			return result;
		} catch (Exception e) {
			LOG.error("unable to get", e);
			return false;
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (httpclient != null) {
			httpclient.close();
		}
	}

	public void setup(String keyword, String username, String password) {
		HttpPost m = new HttpPost(baseUrl + "/api/v1/setup/setup");
		JsonObject json = Json.object();
		json.add("keyword", keyword);
		json.add("username", username);
		json.add("password", password);
		m.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));
		HttpResponse response = null;
		try {
			response = httpclient.execute(m);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != HttpStatus.SC_OK) {
				LOG.info("response: " + EntityUtils.toString(response.getEntity()));
				throw new RuntimeException("invalid status code: " + statusCode);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (response != null) {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
	}

}
