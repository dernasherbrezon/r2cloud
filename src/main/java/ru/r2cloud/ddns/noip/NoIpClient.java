package ru.r2cloud.ddns.noip;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.R2Cloud;

public class NoIpClient {

	private static final Logger LOG = LoggerFactory.getLogger(NoIpClient.class);
	private static final long RETRY_TIMEOUT = TimeUnit.MINUTES.toMillis(30);

	private final HttpClient httpclient;
	private final String hostname;
	private final String username;
	private final String password;

	public NoIpClient(String hostname, String username, String password) {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMinutes(1L)).build();
	}

	public String update(String domain) throws NoIpException, RetryException, InterruptedException {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(hostname + "/nic/update?hostname=" + domain));
		builder.timeout(Duration.ofMinutes(1L));
		builder.header("User-Agent", R2Cloud.getVersion() + " info@r2cloud.ru");
		builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.US_ASCII)));
		HttpResponse<String> response;
		try {
			response = httpclient.send(builder.GET().build(), BodyHandlers.ofString());
		} catch (IOException e) {
			throw new NoIpException("unable to update", e);
		}
		if (response.statusCode() != 200) {
			if (LOG.isErrorEnabled()) {
				LOG.error("unable to update ddns: {}", response.body());
			}
			throw new NoIpException("invalid status code: " + response.statusCode());
		}
		String body = response.body();
		if (body.startsWith("good") || body.startsWith("nochg")) {
			int index = body.indexOf(' ');
			if (index != -1) {
				return body.substring(index + 1);
			}
			throw new NoIpException("unable to get ip from the response: " + body);
		} else if ("nohost".equals(body) || "badauth".equals(body) || "badagent".equals(body) || "!donator".equals(body) || "abuse".equals(body)) {
			throw new NoIpException("fatal error detected: " + body);
		} else if ("911".equals(body)) {
			throw new RetryException(RETRY_TIMEOUT);
		} else {
			throw new NoIpException("unknown response code: " + body);
		}
	}

}
