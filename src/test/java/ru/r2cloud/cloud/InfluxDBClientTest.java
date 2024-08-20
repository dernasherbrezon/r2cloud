package ru.r2cloud.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.FixedClock;
import ru.r2cloud.InfluxDbMock;
import ru.r2cloud.TestConfiguration;
import ru.r2cloud.model.AntennaConfiguration;
import ru.r2cloud.model.AntennaType;
import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.IntegrationConfiguration;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Tle;
import ru.r2cloud.predict.PredictOreKit;

public class InfluxDBClientTest {

	private InfluxDbMock server;
	private InfluxDBClient client;

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSendObservation() throws Exception {
		AntennaConfiguration antenna = new AntennaConfiguration();
		antenna.setType(AntennaType.DIRECTIONAL);

		DeviceConfiguration device = new DeviceConfiguration();
		device.setId("rtlsdr.0");
		device.setAntennaConfiguration(antenna);

		Tle tle = new Tle(new String[] { "METEOR-M 2", "1 40069U 14037A   20271.10206173 -.00000043  00000-0 -42512-6 0  9995", "2 40069  98.4943 304.7538 0005678 359.8115   0.3056 14.20675232322649" });
		tle.setLastUpdateTime(1601173618133L);

		Observation observation = new Observation();
		observation.setDevice(device);
		observation.setTle(tle);
		observation.setStartTimeMillis(1601173648133L);
		observation.setEndTimeMillis(1601173678133L);
		observation.setNumberOfDecodedPackets(10L);

		Satellite satellite = new Satellite();
		satellite.setName("CAS-5A (FO-118)");
		client.send(observation, satellite);
		server.waitForMetric();
		List<String> metrics = server.getMetricsByDatabase().get("r2cloud");
		assertNotNull(metrics);
		assertEquals(1, metrics.size());
		assertEquals("observation,satellite=CAS-5A(FO-118),deviceId=rtlsdr.0,antennaType=DIRECTIONAL,hostname=test.local tleUpdateLatency=30000,tleEpochLatency=30000,numberOfDecodedPackets=10,duration=30000 1601173648133000000", metrics.get(0));
	}

	@Test
	public void testSendJvm() throws Exception {
		client.sendJvm();
		server.waitForMetric();
		List<String> metrics = server.getMetricsByDatabase().get("r2cloud");
		assertNotNull(metrics);
		assertEquals(1, metrics.size());
		Pattern p = Pattern.compile("jvm,hostname=test\\.local heapMemory=\\d+,totalThreads=\\d+ 1601173658133000000");
		assertTrue(p.matcher(metrics.get(0)).find());
	}

	@Before
	public void start() throws Exception {
		server = new InfluxDbMock();
		server.start();
		TestConfiguration config = new TestConfiguration(tempFolder);
		IntegrationConfiguration influxConfig = new IntegrationConfiguration();
		influxConfig.setInfluxdbDatabase("r2cloud");
		influxConfig.setInfluxdbHostname(server.getHostname());
		influxConfig.setInfluxdbPort(server.getPort());
		influxConfig.setInfluxdbUsername(UUID.randomUUID().toString());
		influxConfig.setInfluxdbPassword(UUID.randomUUID().toString());
		config.saveIntegrationConfiguration(influxConfig);
		config.setProperty("local.hostname", "test.local");
		// this will load reuqired libs for TLE parsing
		new PredictOreKit(config);
		client = new InfluxDBClient(config, new FixedClock(1601173658133L));
	}

	@After
	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

}
