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

	private FramingFilter filter;
	private TestConfiguration config;
	private ProcessFactoryMock processFactory;

	@Test
	public void testSuccess() {
		filter = new FramingFilter(config, processFactory);
		Transmitter transmitter = new Transmitter();
		transmitter.setFraming(Framing.APT);
		assertTrue(filter.accept(transmitter));
		transmitter.setFraming(Framing.SATDUMP);
		assertTrue(filter.accept(transmitter));
		
		config.setProperty("satellites.satdump.path", UUID.randomUUID().toString());
		config.setProperty("satellites.wxtoimg.path", UUID.randomUUID().toString());
		filter = new FramingFilter(config, processFactory);
		transmitter.setFraming(Framing.APT);
		assertFalse(filter.accept(transmitter));
		transmitter.setFraming(Framing.SATDUMP);
		assertFalse(filter.accept(transmitter));
	}

	@Before
	public void start() throws Exception {
		String satdump = UUID.randomUUID().toString();
		String wxtoimg = UUID.randomUUID().toString();
		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		mocks.put(satdump, new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), 0, false));
		mocks.put(wxtoimg, new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), 0, false));
		processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		config = new TestConfiguration(tempFolder);
		config.setProperty("satellites.satdump.path", satdump);
		config.setProperty("satellites.wxtoimg.path", wxtoimg);
	}

}
