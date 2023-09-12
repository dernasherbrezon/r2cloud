package ru.r2cloud.satellite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterStatus;

public class SdrTransmitterFilterTest {

	@Test
	public void testSuccess() {
		DeviceConfiguration config = new DeviceConfiguration();
		config.setMinimumFrequency(100_000_000);
		config.setMaximumFrequency(180_000_000);
		SdrTransmitterFilter filter = new SdrTransmitterFilter(config);
		Transmitter satellite = createValid();
		assertTrue(filter.accept(satellite));
		satellite.setFrequency(433_000_000);
		assertFalse(filter.accept(satellite));
		satellite = createValid();
		satellite.setModulation(Modulation.LORA);
		assertFalse(filter.accept(satellite));
		satellite = createValid();
		satellite.setStatus(TransmitterStatus.DECAYED);
		assertFalse(filter.accept(satellite));
		satellite = createValid();
		satellite.setStatus(TransmitterStatus.DISABLED);
		assertFalse(filter.accept(satellite));
		satellite = createValid();
		satellite.setStatus(TransmitterStatus.WEAK);
		assertTrue(filter.accept(satellite));
	}

	private static Transmitter createValid() {
		Transmitter result = new Transmitter();
		result.setFrequency(144_000_000);
		result.setModulation(Modulation.GFSK);
		result.setEnabled(true);
		result.setStatus(TransmitterStatus.ENABLED);
		return result;
	}

}
