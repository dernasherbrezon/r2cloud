package ru.r2cloud.cloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.DefaultTrustManager;
import ru.r2cloud.model.ObservationFull;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class R2CloudClient {

	private static final Logger LOG = LoggerFactory.getLogger(R2CloudClient.class);

	private final Configuration config;

	public R2CloudClient(Configuration config) {
		this.config = config;
		setupTrustAll();
	}

	public Long saveMeta(ObservationFull observation) {
		HttpURLConnection con = null;
		try {
			URL obj = new URL(config.getProperty("r2cloud.hostname") + "/api/v1/observation");
			con = (HttpURLConnection) obj.openConnection();
			con.setConnectTimeout(config.getInteger("r2cloud.connectionTimeout"));
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			con.setRequestProperty("Authorization", config.getProperty("r2cloud.apiKey"));
			con.setRequestProperty("Content-Type", "application/json");
			con.setDoOutput(true);

			JsonObject json = observation.toJson();
			Writer w = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8);
			json.writeTo(w);
			w.close();

			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to save meta. response code: {}. See logs for details", responseCode);
				Util.toLog(LOG, con.getErrorStream());
				return null;
			}
			return readObservationId(con);
		} catch (Exception e) {
			LOG.error("unable to save meta", e);
			return null;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	public void saveJpeg(Long id, File getaPath) {
		upload("/api/v1/observation/" + id + "/data", getaPath, "image/jpeg");
	}

	public void saveBinary(Long id, File getaPath) {
		upload("/api/v1/observation/" + id + "/data", getaPath, "application/octet-stream");
	}
	
	public void saveSpectogram(Long id, File spectogramPath) {
		upload("/api/v1/observation/" + id + "/spectogram", spectogramPath, "image/png");
	}

	private void upload(String url, File file, String contentType) {
		HttpURLConnection con = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			URL obj = new URL(config.getProperty("r2cloud.hostname") + url);
			con = (HttpURLConnection) obj.openConnection();
			con.setDoOutput(true);
			con.setConnectTimeout(config.getInteger("r2cloud.connectionTimeout"));
			con.setRequestMethod("PUT");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			con.setRequestProperty("Authorization", config.getProperty("r2cloud.apiKey"));
			con.setRequestProperty("Content-Type", contentType);
			Util.copy(fis, con.getOutputStream());
			con.getOutputStream().flush();

			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to upload. response code: {}. See logs for details", responseCode);
				Util.toLog(LOG, con.getErrorStream());
			}
		} catch (Exception e) {
			LOG.error("unable to save meta", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					LOG.error("unable to close", e);
				}
			}
			if (con != null) {
				con.disconnect();
			}
		}
	}

	private void setupTrustAll() {
		if (!config.getProperty("server.env").equalsIgnoreCase("dev")) {
			return;
		}
		try {
			SSLContext ctx = SSLContext.getInstance("TLSv1.2");
			ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
			SSLContext.setDefault(ctx);
		} catch (Exception e) {
			LOG.info("unable to setup trust all", e);
		}
	}

	private static Long readObservationId(HttpURLConnection con) throws IOException {
		JsonValue result = Json.parse(new InputStreamReader(con.getInputStream()));
		if (!result.isObject()) {
			LOG.info("malformed json");
			return null;
		}
		JsonObject resultObj = result.asObject();
		String status = resultObj.getString("status", null);
		if (status == null || !status.equalsIgnoreCase("SUCCESS")) {
			LOG.info("response error: {}", resultObj);
			return null;
		}
		long id = resultObj.getLong("id", -1);
		if (id == -1) {
			return null;
		}
		return id;
	}

	public void saveMetrics(JsonArray o) {
		HttpURLConnection con = null;
		try {
			URL obj = new URL(config.getProperty("r2cloud.hostname") + "/api/v1/metrics");
			con = (HttpURLConnection) obj.openConnection();
			con.setConnectTimeout(config.getInteger("r2cloud.connectionTimeout"));
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "r2cloud/0.1 info@r2cloud.ru");
			con.setRequestProperty("Authorization", config.getProperty("r2cloud.apiKey"));
			con.setRequestProperty("Content-Type", "application/json");
			con.setDoOutput(true);

			Writer w = new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8);
			o.writeTo(w);
			w.close();

			int responseCode = con.getResponseCode();
			if (responseCode != 200) {
				LOG.error("unable to save meta. response code: {}. See logs for details", responseCode);
				Util.toLog(LOG, con.getErrorStream());
				return;
			}
		} catch (Exception e) {
			LOG.error("unable to save metrics", e);
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}		
	}

}
