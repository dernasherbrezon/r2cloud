package ru.r2cloud.it;

import org.junit.Test;

import ru.r2cloud.TestUtil;
import ru.r2cloud.it.util.RegisteredTest;

public class DeviceConfigTest extends RegisteredTest {

	@Test
	public void testList() throws Exception {
		TestUtil.assertJson("deviceConfig/expectedList.json", client.getDeviceConfigList());
	}
	
	@Test
	public void testLoad() throws Exception {
		TestUtil.assertJson("deviceConfig/expectedLoad.json", client.getDeviceConfigLoad("rtlsdr.0"));
	}
}
