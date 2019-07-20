package ru.r2cloud.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

import com.eclipsesource.json.Json;

import ru.r2cloud.TestUtil;

public class ValidationResultTest {
	
	@Test
	public void testIsEmpty() {
		ValidationResult result = new ValidationResult();
		assertTrue(result.isEmpty());
		result.setGeneral(UUID.randomUUID().toString());
		assertFalse(result.isEmpty());
		result = new ValidationResult();
		result.put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
		assertFalse(result.isEmpty());
	}

	@Test
	public void testJson() {
		ValidationResult result = create();
		TestUtil.assertJson("ValidationResult/success.json", Json.parse(result.toJson()).asObject());
	}

	@Test
	public void testHashcodeEquals() {
		ValidationResult first = create();
		ValidationResult second = create();
		assertEquals(first.hashCode(), second.hashCode());
		assertTrue(first.equals(second));
	}

	private static ValidationResult create() {
		ValidationResult result = new ValidationResult();
		result.setGeneral("test");
		result.put("name", "value");
		return result;
	}
}
