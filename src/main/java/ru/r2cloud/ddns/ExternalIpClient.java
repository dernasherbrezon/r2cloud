package ru.r2cloud.ddns;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;

final class ExternalIpClient {

	private static final Logger LOG = LoggerFactory.getLogger(ExternalIpClient.class);

	private final String host;
	private final HttpClient httpclient;

	public ExternalIpClient(String host) {
		this.host = host;
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMinutes(1L)).build();
	}

	public String getExternalIp() {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(host));
		builder.timeout(Duration.ofMinutes(1L));
		builder.header("User-Agent", R2Cloud.getVersion() + " info@r2cloud.ru");
		HttpResponse<String> response;

		try {
			response = httpclient.send(builder.GET().build(), BodyHandlers.ofString());
		} catch (IOException e) {
			LOG.error("unable to get an external ip", e);
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		if (response.statusCode() != 200) {
			LOG.error("unable to get an external ip: {}", response.statusCode());
			return null;
		}
		return response.body().trim();
	}

}
