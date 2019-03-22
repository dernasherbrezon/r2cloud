package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.it.util.BaseTest;

public class StaticControllerIT extends BaseTest {

	private File file;
	private String fileContents;

	@Test
	public void testFile() {
		assertEquals(fileContents, client.getFile("/api/v1/admin/static/" + file.getName()));
	}
	
	//FIXME file without auth
//
//	@Test
//	public void testFileWithoutAuth() {
//		HttpResponse<String> response = client.getFileUnauth("/api/v1/admin/static/" + file.getName());
//		assertEquals(403, response.statusCode());
//	}
	
	@Test
	public void testGetUnknownFile() {
		HttpResponse<String> response = client.getFileUnauth("/api/v1/admin/static/" + UUID.randomUUID().toString());
		assertEquals(404, response.statusCode());
	}
	
	@Before
	@Override
	public void start() throws Exception {
		super.start();
		File basePath = new File(config.getProperty("server.static.location"));
		file = new File(basePath, UUID.randomUUID().toString());
		fileContents = UUID.randomUUID().toString();
		try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
			w.append(fileContents);
		}
	}

	@After
	public void stop() {
		if (file != null) {
			file.delete();
		}
	}

}
