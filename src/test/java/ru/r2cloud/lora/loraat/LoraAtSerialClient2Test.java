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

public class LoraAtSerialClient2Test {

	@Test
	public void testFailedToGetStatus() {
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialInterface() {

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
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(false, bais, baos), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, status.getDeviceStatus());
	}

	@Test
	public void testCantWriteToPort() {
		ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, bais, new OutputStream() {

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
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, new InputStream() {

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
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtSerialClient2Test.class.getClassLoader().getResourceAsStream("loraat2/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.FAILED, status.getDeviceStatus());
		assertEquals("AT+MINFREQ?\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testStatusSuccess() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, new MultiStreamInputStream("loraat2/successMin.txt", "loraat2/successMax.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraStatus status = client.getStatus();
		assertEquals(DeviceConnectionStatus.CONNECTED, status.getDeviceStatus());
		assertEquals("IDLE", status.getStatus());
		assertEquals(1, status.getConfigs().size());
		ModulationConfig config = status.getConfigs().get(0);
		assertEquals("lora", config.getName());
		assertEquals(863000000, config.getMinFrequency());
		assertEquals(928000000, config.getMaxFrequency());
		assertEquals("AT+MINFREQ?\r\nAT+MAXFREQ?\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testFailToStop() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtSerialClient2Test.class.getClassLoader().getResourceAsStream("loraat2/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.stopObservation();
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("controlled failure", response.getFailureMessage());
		assertEquals("AT+STOPRX\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testSuccessStop() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtSerialClient2Test.class.getClassLoader().getResourceAsStream("loraat2/successStop.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.stopObservation();
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals(1, response.getFrames().size());
		LoraFrame frame = response.getFrames().get(0);
		assertArrayEquals(new byte[] { (byte) 0xCA, (byte) 0xFE }, frame.getData());
		assertEquals(9655, frame.getFrequencyError());
		assertEquals(-137, frame.getRssi());
		assertEquals(3.2, frame.getSnr(), 0.0001f);
		assertEquals(1703760413119L, frame.getTimestamp());
	}
	
	@Test
	public void testFailToSetTime() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, LoraAtSerialClient2Test.class.getClassLoader().getResourceAsStream("loraat2/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("controlled failure", response.getFailureMessage());
		assertEquals("AT+TIME=1649679986400\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testFailToStart() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, new MultiStreamInputStream("loraat2/success.txt", "loraat2/failure.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.FAILURE, response.getStatus());
		assertEquals("controlled failure", response.getFailureMessage());
		assertEquals("AT+TIME=1649679986400\r\nAT+LORARX=433125000,500000,9,7,18,8,0,0,1,1,255\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testRetryAfterFailure() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, new MultiStreamInputStream("loraat2/success.txt", "loraat2/failureAlreadyReceiving.txt", "loraat2/successStop.txt", "loraat2/success.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals("AT+TIME=1649679986400\r\nAT+LORARX=433125000,500000,9,7,18,8,0,0,1,1,255\r\nAT+STOPRX\r\nAT+LORARX=433125000,500000,9,7,18,8,0,0,1,1,255\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	@Test
	public void testSuccessRx() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		LoraAtClient client = new LoraAtSerialClient2(UUID.randomUUID().toString(), 0, new SerialMock(true, new MultiStreamInputStream("loraat2/success.txt", "loraat2/success.txt"), baos), new SteppingClock(1649679986400L, 1000));
		LoraResponse response = client.startObservation(createRequest());
		assertEquals(ResponseStatus.SUCCESS, response.getStatus());
		assertEquals("AT+TIME=1649679986400\r\nAT+LORARX=433125000,500000,9,7,18,8,0,0,1,1,255\r\n", new String(baos.toByteArray(), StandardCharsets.ISO_8859_1));
	}

	private static LoraObservationRequest createRequest() {
		LoraObservationRequest req = new LoraObservationRequest();
		req.setBw(500000);
		req.setCr(7);
		req.setFrequency(433125000);
		req.setGain(0);
		req.setLdro(0);
		req.setPreambleLength(8);
		req.setSf(9);
		req.setSyncword(18);
		req.setUseCrc(true);
		req.setBeaconSizeBytes(255);
		req.setUseExplicitHeader(true);
		return req;
	}
}
