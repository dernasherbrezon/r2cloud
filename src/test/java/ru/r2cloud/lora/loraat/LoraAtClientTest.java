package ru.r2cloud.lora.loraat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.Test;

import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import ru.r2cloud.SteppingClock;
import ru.r2cloud.lora.LoraFrame;
import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.LoraStatus;
import ru.r2cloud.lora.ModulationConfig;
import ru.r2cloud.lora.ResponseStatus;
import ru.r2cloud.model.DeviceConnectionStatus;

public class LoraAtClientTest {

	@Test
	public void testFailedToGetStatus() {
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialInterface() {

			@Override
			public SerialPortInterface getCommPort(String portDescriptor) throws SerialPortInvalidPortException {
				throw new SerialPortInvalidPortException("controlled failure");
			}
		}, new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, status.getDeviceStatus());
	}

	@Test
	public void testCannotBeOpened() {
		ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(false, bais, baos), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, status.getDeviceStatus());
	}

	@Test
	public void testCantWriteToPort() {
		ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, bais, new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				// do nothing
			}

			@Override
			public void write(byte[] b) throws IOException {
				throw new IOException("controlled failure");
			}
		}), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, status.getDeviceStatus());
	}

	@Test
	public void testCantReadFromPort() {
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, new InputStream() {

			@Override
			public int read() throws IOException {
				throw new IOException("controlled failure");
			}
		}, new ByteArrayOutputStream()), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, status.getDeviceStatus());
	}

	@Test
	public void testStatusError() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtClientTest.class.getClassLoader().getResourceAsStream("loraat/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, status.getDeviceStatus());
		assertEquals("AT+CHIP?\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testStatusSuccess() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtClientTest.class.getClassLoader().getResourceAsStream("loraat/successStatus.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.CONNECTED, status.getDeviceStatus());
		assertEquals("IDLE", status.getStatus());
		assertEquals(1, status.getConfigs().size());
		ModulationConfig config = status.getConfigs().get(0);
		assertEquals("lora", config.getName());
		assertEquals(863, config.getMinFrequency(), 0.0f);
		assertEquals(928, config.getMaxFrequency(), 0.0f);
	}

	@Test
	public void testFailToStop() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtClientTest.class.getClassLoader().getResourceAsStream("loraat/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.stopObservation();
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("controlled failure", response.getFailureMessage());
		assertEquals("AT+STOPRX\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testSuccessStop() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtClientTest.class.getClassLoader().getResourceAsStream("loraat/successStop.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.stopObservation();
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals(1, response.getFrames().size());
		LoraFrame frame = response.getFrames().get(0);
		assertArrayEquals(new byte[] { (byte) 0xCA, (byte) 0xFE }, frame.getData());
		assertEquals(13, frame.getFrequencyError());
		assertEquals(-137, frame.getRssi());
		assertEquals(3.2, frame.getSnr(), 0.0001f);
		assertEquals(1605980902, frame.getTimestamp());
	}

	@Test
	public void testFailToSetTime() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtClientTest.class.getClassLoader().getResourceAsStream("loraat/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("controlled failure", response.getFailureMessage());
		assertEquals("AT+TIME=1649679986\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testFailToStart() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, new MultiStreamInputStream("loraat/success.txt", "loraat/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("controlled failure", response.getFailureMessage());
		assertEquals("AT+TIME=1649679986\r\nAT+LORARX=433.125,500.0,9,7,18,10,8,0,0\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}
	
	@Test
	public void testRetryAfterFailure() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, new MultiStreamInputStream("loraat/success.txt", "loraat/failureAlreadyReceiving.txt", "loraat/successStop.txt", "loraat/success.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals("AT+TIME=1649679986\r\nAT+LORARX=433.125,500.0,9,7,18,10,8,0,0\r\nAT+STOPRX\r\nAT+LORARX=433.125,500.0,9,7,18,10,8,0,0\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}
	
	@Test
	public void testSuccessRx() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient(UUID.randomUUID().toString(), 0, new SerialMock(true, new MultiStreamInputStream("loraat/success.txt", "loraat/success.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals("AT+TIME=1649679986\r\nAT+LORARX=433.125,500.0,9,7,18,10,8,0,0\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	private static LoraObservationRequest createRequest() {
		LoraObservationRequest req = new LoraObservationRequest();
		req.setBw(500.0f);
		req.setCr(7);
		req.setFrequency(433.125f);
		req.setGain(0);
		req.setLdro(0);
		req.setPreambleLength(8);
		req.setSf(9);
		req.setSyncword(18);
		return req;
	}
}
