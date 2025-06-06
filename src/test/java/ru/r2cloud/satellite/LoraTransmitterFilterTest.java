package ru.r2cloud.satellite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ru.r2cloud.NoOpTransmitterFilter;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterStatus;

public class LoraTransmitterFilterTest {

	@Test
	public void testSuccess() {
		DeviceConfiguration config = new DeviceConfiguration();
		config.setMinimumFrequency(432_000_000);
		config.setMaximumFrequency(450_000_000);
		LoraTransmitterFilter filter = new LoraTransmitterFilter(config, new NoOpTransmitterFilter());
		Transmitter satellite = createValid();
		assertTrue(filter.accept(satellite));
		satellite.setFrequency(868_000_000);
		assertFalse(filter.accept(satellite));
		satellite = createValid();
		satellite.setModulation(Modulation.GFSK);
		assertFalse(filter.accept(satellite));
	}

	private static Transmitter createValid() {
		Transmitter result = new Transmitter();
		result.setFrequency(433_000_000);
		result.setModulation(Modulation.LORA);
		result.setEnabled(true);
		result.setStatus(TransmitterStatus.ENABLED);
		return result;
	}
	
}
