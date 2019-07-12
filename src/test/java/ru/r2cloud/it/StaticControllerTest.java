package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import ru.r2cloud.it.util.BaseTest;

public class StaticControllerTest extends BaseTest {

	@Test
	public void testFileWithoutAuth() {
		HttpResponse<String> response = client.getFileResponse("/api/v1/admin/static/" + UUID.randomUUID().toString());
		assertEquals(401, response.statusCode());
	}

}
