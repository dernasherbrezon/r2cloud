package ru.r2cloud.predict;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.util.Util;

class OreKitDataClient {

	private static final Logger LOG = LoggerFactory.getLogger(OreKitDataClient.class);
	private static final int TIMEOUT = 10000;
	private static final String USER_AGENT = "r2cloud/0.1 info@r2cloud.ru";

	private final HttpClient httpclient;
	private final List<String> urls;

	public OreKitDataClient(List<String> urls) {
		if (urls == null || urls.isEmpty()) {
			throw new IllegalArgumentException("urls are blank. at least 1 is expected");
		}
		this.urls = urls;
		this.httpclient = HttpClient.newBuilder().version(Version.HTTP_2).followRedirects(Redirect.NORMAL).connectTimeout(Duration.ofMillis(TIMEOUT)).build();
	}

	public void downloadAndSaveTo(Path dst) throws IOException {
		IOException lastException = null;
		for (String cur : urls) {
			try {
				downloadAndSaveTo(cur, dst);
				return;
			} catch (IOException e) {
				LOG.info("unable to download from: {} error: {}", cur, e.getMessage());
				lastException = e;
			}
		}
		// safe check
		if (lastException != null) {
			throw lastException;
		}
	}

	private void downloadAndSaveTo(String url, Path dst) throws IOException {
		Path tempPath = dst.getParent().resolve(dst.getFileName() + ".tmp").normalize();
		if (Files.exists(tempPath) && !Util.deleteDirectory(tempPath)) {
			throw new RuntimeException("unable to delete tmp directory: " + tempPath);
		}
		Files.createDirectories(tempPath);
		Builder result = HttpRequest.newBuilder().uri(URI.create(url));
		result.timeout(Duration.ofMillis(TIMEOUT));
		result.header("User-Agent", USER_AGENT);
		HttpRequest request = result.build();
		try {
			HttpResponse<InputStream> response = httpclient.send(request, BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new IOException("invalid status code: " + response.statusCode());
			}
			Optional<String> contentType = response.headers().firstValue("Content-Type");
			if (contentType.isEmpty() || !contentType.get().equals("application/zip")) {
				throw new IOException("Content-Type is empty or unsupported: " + contentType);
			}
			try (ZipInputStream zis = new ZipInputStream(response.body())) {
				ZipEntry zipEntry = null;
				while ((zipEntry = zis.getNextEntry()) != null) {
					Path destFile = tempPath.resolve(zipEntry.getName()).normalize();
					if (!destFile.startsWith(tempPath)) {
						throw new IOException("invalid archive. zip slip detected: " + destFile);
					}
					if (zipEntry.isDirectory()) {
						Files.createDirectories(destFile);
						continue;
					}
					if (!Files.exists(destFile.getParent())) {
						Files.createDirectories(destFile.getParent());
					}
					Files.copy(zis, destFile, StandardCopyOption.REPLACE_EXISTING);
				}

				Files.move(tempPath, dst, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

}
