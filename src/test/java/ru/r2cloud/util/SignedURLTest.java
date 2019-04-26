package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;

public class SignedURLTest {

	private static final String path = "/img/a.jpg";

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private SignedURL service;
	private Clock clock;
	private TestConfiguration config;

	@Test
	public void testSuccess() {
		String signed = service.sign(path);
		assertEquals("/img/a.jpg?hash=3679d97d8b1d497743cd8da8ba0440f5&timestamp=1508716800000", signed);
		assertTrue(service.validate(path, createParameters("hash", "3679d97d8b1d497743cd8da8ba0440f5", "timestamp", "1508716800000")));
	}

	@Test
	public void testCornerCases() {
		assertNull(service.sign(null));
		assertFalse(service.validate(null, new HashMap<>()));
		assertFalse(service.validate(UUID.randomUUID().toString(), new HashMap<>()));
		assertFalse(service.validate(UUID.randomUUID().toString(), createParameters("test", "test")));
	}

	@Test
	public void testInvalidHash() {
		assertFalse(service.validate(path, createParameters("hash", "3679d97d8b1d497743cd8da8ba0440f6", "timestamp", "1508716800000")));
	}
	
	@Test
	public void testChangePassword() {
		config.setProperty("server.password", UUID.randomUUID().toString());
		assertFalse(service.validate(path, createParameters("hash", "3679d97d8b1d497743cd8da8ba0440f5", "timestamp", "1508716800000")));
	}
	
	@Test
	public void testExpiredUrl() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss, SSS");
		long current = sdf.parse("2018-10-23 00:00:00, 000").getTime();
		when(clock.millis()).thenReturn(current);

		assertFalse(service.validate(path, createParameters("hash", "3679d97d8b1d497743cd8da8ba0440f5", "timestamp", "1508716800000")));
	}

	@Before
	public void start() throws Exception {
		clock = mock(Clock.class);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss, SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		long current = sdf.parse("2017-10-23 00:00:00, 000").getTime();
		when(clock.millis()).thenReturn(current);

		config = new TestConfiguration(tempFolder);

		service = new SignedURL(config, clock);
	}

	private static Map<String, List<String>> createParameters(String... str) {
		Map<String, List<String>> result = new HashMap<>();
		for (int i = 0; i < str.length; i += 2) {
			result.put(str[i], Collections.singletonList(str[i + 1]));
		}
		return result;
	}
}
