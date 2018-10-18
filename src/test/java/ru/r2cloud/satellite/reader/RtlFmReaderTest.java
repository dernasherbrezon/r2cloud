package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.ProcessWrapperMock;
import ru.r2cloud.satellite.reader.RtlFmReader;
import ru.r2cloud.util.ProcessFactory;

public class RtlFmReaderTest {

	private static final Logger LOG = LoggerFactory.getLogger(RtlFmReaderTest.class);

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private Satellite satellite;
	private ProcessFactory factory;

	@Test
	public void testSuccess() throws Exception {
		String sox = UUID.randomUUID().toString();
		String rtlfm = UUID.randomUUID().toString();
		config.setProperty("satellites.sox.path", sox);
		config.setProperty("satellites.rtlfm.path", rtlfm);

		String data = UUID.randomUUID().toString();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

		when(factory.create(contains(sox), any(), anyBoolean())).thenReturn(new ProcessWrapperMock(null, baos));
		when(factory.create(contains(rtlfm), any(), anyBoolean())).thenReturn(new ProcessWrapperMock(bais, null));

		ObservationRequest req = new ObservationRequest();

		RtlFmReader o = new RtlFmReader(config, factory, req);
		o.start();
		IQData result = o.stop();
		assertNotNull(result.getWavFile());
		assertTrue(result.getWavFile().exists());
		if (!result.getWavFile().delete()) {
			LOG.error("unable to delete temp file: " + result.getWavFile().getAbsolutePath());
		}

		verify(factory, times(2)).create(any(), any(), anyBoolean());
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.enabled", true);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		satellite = new Satellite();
		satellite.setId(UUID.randomUUID().toString());
		satellite.setFrequency(10);
		satellite.setName(UUID.randomUUID().toString());

		factory = mock(ProcessFactory.class);
	}

}
