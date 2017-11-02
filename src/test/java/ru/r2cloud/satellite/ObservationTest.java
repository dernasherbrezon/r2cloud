package ru.r2cloud.satellite;

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
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.SatPass;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.util.ProcessFactory;
import uk.me.g4dpz.satellite.SatPos;

public class ObservationTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private Satellite satellite;
	private ProcessFactory factory;

	@Test
	public void testSuccess() throws Exception {
		String sox = UUID.randomUUID().toString();
		String rtlfm = UUID.randomUUID().toString();
		String wxtoimg = UUID.randomUUID().toString();
		config.setProperty("satellites.sox.path", sox);
		config.setProperty("satellites.rtlsdr.path", rtlfm);
		config.setProperty("satellites.wxtoimg.path", wxtoimg);

		String data = UUID.randomUUID().toString();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

		when(factory.create(contains(sox), any(), anyBoolean())).thenReturn(new ProcessWrapperMock(null, baos));
		when(factory.create(contains(rtlfm), any(), anyBoolean())).thenReturn(new ProcessWrapperMock(bais, null));

		SatPass nextPass = create(new Date(), new Date());
		Observation o = new Observation(config, satellite, nextPass, factory);
		o.start();

		try (FileOutputStream fos = new FileOutputStream(new File(tempFolder.getRoot(), satellite.getId() + File.separator + "data" + File.separator + nextPass.getStart().getTime().getTime()) + File.separator + "output.wav")) {
			fos.write(0);
		}

		assertEquals(data, new String(baos.toByteArray(), StandardCharsets.UTF_8));

		when(factory.create(contains(wxtoimg), any(), anyBoolean())).thenReturn(new ProcessWrapperMock(null, null));
		o.stop();

		verify(factory, times(4)).create(any(), any(), anyBoolean());

	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration();
		config.setProperty("satellites.enabled", true);
		config.setProperty("satellites.basepath.location", tempFolder.getRoot().getAbsolutePath());
		config.update();

		satellite = new Satellite();
		satellite.setId(UUID.randomUUID().toString());
		satellite.setFrequency(10);
		satellite.setName(UUID.randomUUID().toString());

		factory = mock(ProcessFactory.class);
	}

	private static SatPass create(Date start, Date end) {
		SatPos startPos = new SatPos();
		startPos.setTime(start);
		SatPos endPos = new SatPos();
		endPos.setTime(end);
		SatPass result = new SatPass();
		result.setStart(startPos);
		result.setEnd(endPos);
		return result;
	}
}
