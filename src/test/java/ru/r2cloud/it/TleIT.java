package ru.r2cloud.it;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ru.r2cloud.it.util.RegisteredIT;

public class TleIT extends RegisteredIT {
	
	@Test
	public void testGetTle() {
		assertNotNull(client.getTle());
	}
	
}
