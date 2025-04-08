package ru.r2cloud.satellite.decoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.jradio.BeaconOutputStream;
import ru.r2cloud.jradio.RawBeacon;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;

public class LoraDecoderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSuccess() throws Exception {
		RawBeacon beacon = new RawBeacon();
		beacon.setBeginMillis(1641987504000L);
		beacon.setRawData(new byte[] { 0x11, 0x22 });
		File rawFile = new File(tempFolder.getRoot(), UUID.randomUUID().toString() + ".raw");
		try (BeaconOutputStream bos = new BeaconOutputStream(new FileOutputStream(rawFile))) {
			bos.write(beacon);
		}

		LoraDecoder decoder = new LoraDecoder(RawBeacon.class);
		DecoderResult result = decoder.decode(rawFile, new Observation(), new Transmitter(), new Satellite());
		assertNotNull(result);
		assertEquals(1, result.getNumberOfDecodedPackets());
		assertNotNull(result.getDataPath());
	}

	@Test
	public void testNoDataOrInvalid() throws Exception {
		File rawFile = new File(tempFolder.getRoot(), UUID.randomUUID().toString() + ".raw");
		try (FileOutputStream fos = new FileOutputStream(rawFile)) {
			fos.write(1);
		}
		LoraDecoder decoder = new LoraDecoder(RawBeacon.class);
		DecoderResult result = decoder.decode(rawFile, new Observation(), new Transmitter(), new Satellite());
		assertNotNull(result);
		assertEquals(0, result.getNumberOfDecodedPackets());
		assertNull(result.getDataPath());
		assertFalse(rawFile.exists());
	}

}
