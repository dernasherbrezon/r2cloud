package ru.r2cloud.satellite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Transmitter;

public class FramingFilterTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private TestConfiguration config;
	private ProcessFactoryMock processFactory;
	private ProcessWrapperMock satdumpMock;
	private ProcessWrapperMock wxtoimgMock;

	@Test
	public void testSuccess() {
		assertExternalTool("satellites.satdump.path", satdumpMock, Framing.SATDUMP);
		assertExternalTool("satellites.wxtoimg.path", wxtoimgMock, Framing.APT);

		FramingFilter filter = new FramingFilter(config, processFactory);
		Transmitter transmitter = new Transmitter();
		assertTrue(filter.accept(transmitter));

		transmitter.setFraming(Framing.SATDUMP);
		satdumpMock.setStatusCode(1);
		config.setProperty("satellits.validate.external", false);
		assertTrue(filter.accept(transmitter));
	}

	private void assertExternalTool(String configName, ProcessWrapperMock mock, Framing framing) {
		FramingFilter filter = new FramingFilter(config, processFactory);
		Transmitter transmitter = new Transmitter();
		transmitter.setFraming(framing);
		assertTrue(filter.accept(transmitter));
		
		filter = new FramingFilter(config, processFactory);
		mock.setStatusCode(1);
		assertFalse(filter.accept(transmitter));
		config.setProperty(configName, UUID.randomUUID().toString());
		filter = new FramingFilter(config, processFactory);
		assertFalse(filter.accept(transmitter));
	}

	@Before
	public void start() throws Exception {
		String satdump = UUID.randomUUID().toString();
		String wxtoimg = UUID.randomUUID().toString();
		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		satdumpMock = new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), 0, false);
		wxtoimgMock = new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), 0, false);
		mocks.put(satdump, satdumpMock);
		mocks.put(wxtoimg, wxtoimgMock);
		processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.satdump.path", satdump);
		config.setProperty("satellites.wxtoimg.path", wxtoimg);
	}

}
