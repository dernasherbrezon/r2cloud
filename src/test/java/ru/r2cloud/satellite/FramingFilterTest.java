package ru.r2cloud.satellite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

	@Test
	public void testSuccess() {
		assertExternalTool("satellites.satdump.available", Framing.SATDUMP);
		assertExternalTool("satellites.wxtoimg.available", Framing.APT);

		FramingFilter filter = new FramingFilter(config);
		Transmitter transmitter = new Transmitter();
		assertTrue(filter.accept(transmitter));

		config.setProperty("satellites.satdump.available", false);
		config.setProperty("satellites.validate.external", false);
		filter = new FramingFilter(config);
		transmitter.setFraming(Framing.SATDUMP);
		assertTrue(filter.accept(transmitter));
	}

	private void assertExternalTool(String appConfigName, Framing framing) {
		config.setProperty(appConfigName, true);
		FramingFilter filter = new FramingFilter(config);
		Transmitter transmitter = new Transmitter();
		transmitter.setFraming(framing);
		assertTrue(filter.accept(transmitter));

		config.setProperty(appConfigName, false);
		filter = new FramingFilter(config);
		assertFalse(filter.accept(transmitter));
	}

	@Before
	public void start() throws Exception {
		config = new TestConfiguration(tempFolder);
	}

}
