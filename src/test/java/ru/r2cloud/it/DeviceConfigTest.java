package ru.r2cloud.it;

import static org.junit.Assert.assertEquals;

import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;
import ru.r2cloud.model.AirspyGainType;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.RotatorConfiguration;
import ru.r2cloud.model.SdrServerConfiguration;

public class DeviceConfigTest extends RegisteredTest {

	@Test
	public void testList() throws Exception {
		TestUtil.assertJson("deviceConfig/expectedList.json", client.getDeviceConfigList());
	}

	@Test
	public void testLoad() throws Exception {
		TestUtil.assertJson("deviceConfig/expectedLoad.json", client.getDeviceConfigLoad("rtlsdr.0"));
	}

	@Test
	public void testSaveRtlsdr() throws Exception {
		DeviceConfiguration device = createConfig();
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(200, response.statusCode());
		JsonObject json = Json.parse(response.body()).asObject();
		assertEquals("rtlsdr.1", json.getString("id", null));

		device = createConfig();
		device.setGain(50.0f);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("gain", response);

		device = createConfig();
		device.setMaximumFrequency(1_777_000_000L);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("maximumFrequency", response);

		device = createConfig();
		device.setMinimumFrequency(400_000L);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("minimumFrequency", response);

	}

	@Test
	public void testSaveLoraAt() throws Exception {
		DeviceConfiguration device = createConfig();
		device.setDeviceType(DeviceType.LORAAT);
		device.setSerialDevice("/dev/ttyUSB0");
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(200, response.statusCode());
		assertEquals("loraat.0", Json.parse(response.body()).asObject().getString("id", null));

		device = createConfig();
		device.setDeviceType(DeviceType.LORAAT);
		device.setSerialDevice(null);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("serialDevice", response);

		device = createConfig();
		device.setDeviceType(DeviceType.LORAAT);
		device.setSerialDevice("/dev/ttyUSB0");
		device.setGain(7.0f);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("gain", response);
	}

	@Test
	public void testSaveLoraAtBle() throws Exception {
		DeviceConfiguration device = createLoraAtBleConfig();
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(200, response.statusCode());
		assertEquals("loraatble.0", Json.parse(response.body()).asObject().getString("id", null));

		device = createLoraAtBleConfig();
		device.setBtAddress(null);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("btAddress", response);

		device = createLoraAtBleConfig();
		device.setGain(7.0f);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("gain", response);

		device = createLoraAtBleConfig();
		device.setMaximumBatteryVoltage(3.0);
		device.setMinimumBatteryVoltage(4.2);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("minimumBatteryVoltage", response);
	}

	@Test
	public void testSaveLoraAtWifi() throws Exception {
		DeviceConfiguration device = createLoraAtWifiConfig();
		JsonObject json = device.toJson();
		json.add("password", UUID.randomUUID().toString());
		HttpResponse<String> response = client.saveDeviceConfig(json);
		assertEquals(200, response.statusCode());
		assertEquals("loraatwifi.0", Json.parse(response.body()).asObject().getString("id", null));

		device = createLoraAtWifiConfig();
		device.setHost(null);
		json = device.toJson();
		json.add("password", UUID.randomUUID().toString());
		response = client.saveDeviceConfig(json);
		assertEquals(400, response.statusCode());
		assertErrorInField("host", response);

		device = createLoraAtWifiConfig();
		device.setHost(UUID.randomUUID().toString());
		json = device.toJson();
		json.add("password", UUID.randomUUID().toString());
		response = client.saveDeviceConfig(json);
		assertEquals(400, response.statusCode());
		assertErrorInField("host", response);

		device = createLoraAtWifiConfig();
		device.setUsername(null);
		json = device.toJson();
		json.add("password", UUID.randomUUID().toString());
		response = client.saveDeviceConfig(json);
		assertEquals(400, response.statusCode());
		assertErrorInField("username", response);

		device = createLoraAtWifiConfig();
		device.setPassword(null);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("password", response);

		device = createLoraAtWifiConfig();
		device.setGain(7.0f);
		json = device.toJson();
		json.add("password", UUID.randomUUID().toString());
		response = client.saveDeviceConfig(json);
		assertEquals(400, response.statusCode());
		assertErrorInField("gain", response);
	}

	@Test
	public void testSaveSdrServer() throws Exception {
		DeviceConfiguration device = createSdrServerConfig();
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(200, response.statusCode());
		assertEquals("sdrserver.0", Json.parse(response.body()).asObject().getString("id", null));

		device = createSdrServerConfig();
		device.getSdrServerConfiguration().setBasepath(null);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("basepath", response);
	}

	@Test
	public void testSaveSpyServer() throws Exception {
		DeviceConfiguration device = createConfig();
		device.setDeviceType(DeviceType.SPYSERVER);
		device.setHost("localhost");
		device.setPort(8080);
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(200, response.statusCode());
		assertEquals("spyserver.0", Json.parse(response.body()).asObject().getString("id", null));
	}

	@Test
	public void testSavePlutosdr() throws Exception {
		DeviceConfiguration device = createConfig();
		device.setDeviceType(DeviceType.PLUTOSDR);
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(200, response.statusCode());
		assertEquals("plutosdr.0", Json.parse(response.body()).asObject().getString("id", null));
	}

	@Test
	public void testSaveAirspy() throws Exception {
		DeviceConfiguration device = createAirspyConfig();
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(200, response.statusCode());
		assertEquals("airspy.0", Json.parse(response.body()).asObject().getString("id", null));

		device = createAirspyConfig();
		device.setMaximumFrequency(1_777_000_000L);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("maximumFrequency", response);

		device = createAirspyConfig();
		device.setMinimumFrequency(1_000_000L);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("minimumFrequency", response);

		device = createAirspyConfig();
		device.setGainType(null);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("gainType", response);

		device = createAirspyConfig();
		device.setGainType(AirspyGainType.SENSITIVE);
		device.setGain(22);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("gain", response);

		device = createAirspyConfig();
		device.setGainType(AirspyGainType.FREE);
		device.setVgaGain(16);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("vgaGain", response);

		device = createAirspyConfig();
		device.setGainType(AirspyGainType.FREE);
		device.setMixerGain(16);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("mixerGain", response);

		device = createAirspyConfig();
		device.setGainType(AirspyGainType.FREE);
		device.setLnaGain(16);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("lnaGain", response);

	}

	@Test
	public void testInvalidArguments() throws Exception {
		DeviceConfiguration device = createConfig();
		device.setMaximumFrequency(device.getMinimumFrequency() - 100);
		HttpResponse<String> response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("minimumFrequency", response);

		device = createConfig();
		device.setId(UUID.randomUUID().toString());
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("general", response);

		device = createConfig();
		device.setDeviceType(null);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("deviceType", response);

		device = createConfig();
		device.setAntennaConfiguration(null);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("antenna.antennaType", response);

		device = createConfig();
		device.getAntennaConfiguration().setType(AntennaType.FIXED_DIRECTIONAL);
		device.getAntennaConfiguration().setAzimuth(390.0);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("antenna.azimuth", response);

		device = createConfig();
		device.getAntennaConfiguration().setType(AntennaType.FIXED_DIRECTIONAL);
		device.getAntennaConfiguration().setElevation(91.0);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("antenna.elevation", response);

		device = createConfig();
		device.getAntennaConfiguration().setType(AntennaType.FIXED_DIRECTIONAL);
		device.getAntennaConfiguration().setBeamwidth(390.0);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("antenna.beamwidth", response);

		device = createConfig();
		device.getAntennaConfiguration().setMinElevation(91.0);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("antenna.minElevation", response);

		device = createConfig();
		device.getAntennaConfiguration().setGuaranteedElevation(91.0);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("antenna.guaranteedElevation", response);

		device = createConfig();
		device.getAntennaConfiguration().setMinElevation(15.0);
		device.getAntennaConfiguration().setGuaranteedElevation(10.0);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("antenna.minElevation", response);

		device = createConfig();
		device.getAntennaConfiguration().setType(AntennaType.DIRECTIONAL);
		device.setRotatorConfiguration(new RotatorConfiguration());
		device.getRotatorConfiguration().setTolerance(390.0);
		response = client.saveDeviceConfig(device.toJson());
		assertEquals(400, response.statusCode());
		assertErrorInField("rotator.rotatorTolerance", response);
	}

	private static DeviceConfiguration createLoraAtWifiConfig() {
		DeviceConfiguration device = createConfig();
		device.setDeviceType(DeviceType.LORAATWIFI);
		device.setHost("localhost");
		device.setPort(8080);
		device.setUsername(UUID.randomUUID().toString());
		return device;
	}

	private static DeviceConfiguration createLoraAtBleConfig() {
		DeviceConfiguration device = createConfig();
		device.setDeviceType(DeviceType.LORAATBLE);
		device.setBtAddress("78:DD:08:A3:A7:52");
		device.setMaximumBatteryVoltage(4.1f);
		device.setMinimumBatteryVoltage(3.0f);
		return device;
	}
	
	private static DeviceConfiguration createSdrServerConfig() {
		DeviceConfiguration device = createConfig();
		device.setDeviceType(DeviceType.SDRSERVER);
		device.setHost("localhost");
		device.setPort(8080);
		SdrServerConfiguration sdrServerConfig = new SdrServerConfiguration();
		sdrServerConfig.setBandwidth(1400_000);
		sdrServerConfig.setBandwidthCrop(48_000);
		sdrServerConfig.setBasepath("/tmp");
		sdrServerConfig.setUseGzip(true);
		device.setSdrServerConfiguration(sdrServerConfig);
		return device;
	}

	private static DeviceConfiguration createConfig() {
		DeviceConfiguration result = new DeviceConfiguration();
		result.setDeviceType(DeviceType.RTLSDR);
		result.setBiast(true);
		AntennaConfiguration antennaConfig = new AntennaConfiguration();
		antennaConfig.setType(AntennaType.OMNIDIRECTIONAL);
		antennaConfig.setMinElevation(5.0);
		antennaConfig.setGuaranteedElevation(15.0);
		result.setAntennaConfiguration(antennaConfig);
		result.setGain(3f);
		result.setPpm(3);
		result.setRtlDeviceId("1");
		result.setMinimumFrequency(400_000_000L);
		result.setMaximumFrequency(1_700_000_000L);
		return result;
	}

	private static DeviceConfiguration createAirspyConfig() {
		DeviceConfiguration result = createConfig();
		result.setDeviceType(DeviceType.AIRSPY);
		result.setGainType(AirspyGainType.LINEAR);
		return result;
	}
}
