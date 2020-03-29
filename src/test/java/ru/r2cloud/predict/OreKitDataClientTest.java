package ru.r2cloud.predict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class OreKitDataClientTest {

	private HttpServer server;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private String validZipFile;
	private Path dataFolder;

	@SuppressWarnings("unused")
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidArguments() {
		new OreKitDataClient(Collections.emptyList());
	}

	@Test(expected = IOException.class)
	public void testInvalidContent() throws Exception {
		server.createContext("/file.zip", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] errorBody = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, errorBody.length);
				OutputStream os = exchange.getResponseBody();
				os.write(errorBody);
				os.close();
			}
		});
		OreKitDataClient client = new OreKitDataClient(urls(validZipFile));
		client.downloadAndSaveTo(dataFolder);
	}

	@Test(expected = IOException.class)
	public void testNoFile() throws Exception {
		OreKitDataClient client = new OreKitDataClient(urls(validZipFile));
		client.downloadAndSaveTo(dataFolder);
	}

	@Test(expected = IOException.class)
	public void testZipSlip() throws Exception {
		setupZipFile("../../test/invalid", UUID.randomUUID().toString());
		OreKitDataClient client = new OreKitDataClient(urls(validZipFile));
		client.downloadAndSaveTo(dataFolder);
	}

	@Test
	public void testTempFolderAlreadyExist() throws Exception {
		Files.createDirectories(dataFolder.getParent().resolve("./orekit-data.tmp/test/some/old/path"));
		testFallbackToSecondUrl();
	}

	@Test
	public void testFallbackToSecondUrl() throws Exception {
		String path = "test/test2/path";
		String data = UUID.randomUUID().toString();
		setupZipFile(path, data);
		OreKitDataClient client = new OreKitDataClient(urls("https://255.255.255.255/invalid.zip", validZipFile));
		client.downloadAndSaveTo(dataFolder);
		Path actualFile = dataFolder.resolve(path);
		assertTrue(Files.exists(actualFile));
		assertFile(actualFile, data);
	}

	private void setupZipFile(String path, String data) {
		server.createContext("/file.zip", new HttpHandler() {

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] zip = createZip(path, data);
				exchange.getResponseHeaders().add("Content-Type", "application/zip");
				exchange.sendResponseHeaders(200, zip.length);
				OutputStream os = exchange.getResponseBody();
				os.write(zip);
				os.close();
			}
		});
	}

	private static byte[] createZip(String filename, String data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(baos)) {
			int slashIndex = filename.indexOf('/');
			// 1 directory entry is enough for test
			if (slashIndex != -1) {
				ZipEntry ze = new ZipEntry(filename.substring(0, slashIndex + 1));
				zos.putNextEntry(ze);
			}
			ZipEntry entry = new ZipEntry(filename);
			zos.putNextEntry(entry);
			zos.write(data.getBytes(StandardCharsets.UTF_8));
		}
		return baos.toByteArray();
	}

	private static void assertFile(Path actualFile, String expectedContents) throws IOException {
		List<String> lines = Files.readAllLines(actualFile);
		assertEquals(1, lines.size());
		assertEquals(expectedContents, lines.get(0));
	}

	@Before
	public void start() throws Exception {
		server = HttpServer.create(new InetSocketAddress("localhost", 8000), 0);
		server.start();
		dataFolder = tempFolder.getRoot().toPath().resolve("./orekit-data");
		validZipFile = "http://" + server.getAddress().getHostName() + ":" + server.getAddress().getPort() + "/file.zip";
	}

	@After
	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	private static List<String> urls(String... data) {
		List<String> result = new ArrayList<>();
		for (String cur : data) {
			result.add(cur);
		}
		return result;
	}
}
