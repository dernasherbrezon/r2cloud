package ru.r2cloud.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HexTest {

	@Test
	public void test() {
		assertEquals("ac1100", Hex.encode(new byte[] { (byte) 0xac, 0x11, 0x00 }));
	}

}
