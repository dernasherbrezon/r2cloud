package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.satellite.ProcessWrapperMock;
import ru.r2cloud.util.ProcessFactory;

public class RtlFmReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ProcessFactory factory;
	private String sox;
	private String rtlfm;

	@Test
	public void testSuccess() throws Exception {
		String data = UUID.randomUUID().toString();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

		when(factory.create(contains(sox), any(), anyBoolean())).thenReturn(new ProcessWrapperMock(null, baos));
		when(factory.create(contains(rtlfm), any(), anyBoolean())).thenReturn(new ProcessWrapperMock(bais, null));

		ObservationRequest req = new ObservationRequest();

		RtlFmReader o = new RtlFmReader(config, factory, req);
		o.start();
		o.complete();
		assertEquals(data, new String(baos.toByteArray(), StandardCharsets.UTF_8));

		verify(factory, times(2)).create(any(), any(), anyBoolean());
	}

	@Before
	public void start() throws Exception {
		sox = UUID.randomUUID().toString();
		rtlfm = UUID.randomUUID().toString();

		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.sox.path", sox);
		config.setProperty("satellites.rtlfm.path", rtlfm);
		config.setProperty("server.tmp.directory", tempFolder.getRoot().getAbsolutePath());
		config.update();

		factory = mock(ProcessFactory.class);
	}

}
