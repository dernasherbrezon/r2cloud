package ru.r2cloud.satellite.reader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystems;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.aerse.mockfs.FailingByteChannelCallback;
import com.aerse.mockfs.MockFileSystem;

import ru.r2cloud.TestConfiguration;
import ru.r2cloud.device.LoraAtBleDevice;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IQData;
import ru.r2cloud.util.Configuration;

public class LoraAtBleReaderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private Configuration config;
	private LoraAtBleReader reader;
	private LoraAtBleDevice device;
	private MockFileSystem fs;

	@Test
	public void testNoFrames() throws Exception {
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNull(iqData);
	}

	@Test
	public void testSuccess() throws Exception {
		device.addFrame(createValidFrame());
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNotNull(iqData);
		assertNotNull(iqData.getIq());
		assertTrue(device.getFrames().isEmpty());
	}

	@Test
	public void testFailedFilesystem() throws Exception {
		fs.mock(config.getTempDirectoryPath(), new FailingByteChannelCallback(3));
		device.addFrame(createValidFrame());
		// make sure we won't stuck in the reader.start
		reader.complete();
		IQData iqData = reader.start();
		assertNull(iqData);
		assertTrue(device.getFrames().isEmpty());
	}

	@Before
	public void start() throws Exception {
		fs = new MockFileSystem(FileSystems.getDefault());
		config = new TestConfiguration(tempFolder, fs);
		DeviceConfiguration deviceConfig = new DeviceConfiguration();
		device = new LoraAtBleDevice(UUID.randomUUID().toString(), null, 0, null, null, null, deviceConfig, null, null, null, null, config);
		reader = new LoraAtBleReader(config, LoraAtReaderTest.createDeviceConfiguration(), LoraAtReaderTest.createValidRequest(), device, LoraAtReaderTest.createValidTransmitter());
	}

	private static LoraFrame createValidFrame() {
		LoraFrame frame = new LoraFrame();
		frame.setData(new byte[] { (byte) 0xCA, (byte) 0xFE });
		return frame;
	}

}
