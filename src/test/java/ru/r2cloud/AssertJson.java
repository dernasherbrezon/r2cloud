package ru.r2cloud;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class AssertJson {

	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Date.class, new DateAdapter()).create();

	public static void assertObjectsEqual(String jsonFilename, Object actualObject, Class<?> clazz) {
		assertNotNull(actualObject);
		Object expected;
		try (InputStreamReader reader = new InputStreamReader(AssertJson.class.getResourceAsStream("/expected/" + jsonFilename), StandardCharsets.UTF_8)) {
			expected = GSON.fromJson(reader, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		StringBuilder message = new StringBuilder();
		assertElements(message, "", expected, actualObject);

		if (message.length() > 0) {
			fail(message.toString());
		}
	}

	public static void assertObjectsEqual(String jsonFilename, Object actualObject) {
		assertObjectsEqual(jsonFilename, actualObject, actualObject.getClass());
	}

	private static void assertElements(StringBuilder message, String fieldName, Object expected, Object actual) {
		if (expected == null && actual != null) {
			message.append("expected " + fieldName + " null. got: " + actual);
			return;
		}
		if (expected != null && actual == null) {
			message.append("expected " + fieldName + " non-null. got null");
			return;
		}
		// could be &&, but using || to be on the safe side
		if (expected == null || actual == null) {
			return;
		}
		if (expected.getClass() != actual.getClass()) {
			message.append("invalid type. " + fieldName + " expected: " + expected.getClass() + " actual: " + actual.getClass() + "\n");
			return;
		}
		if (expected.getClass().isPrimitive()) {
			if (!expected.equals(actual)) {
				message.append(fieldName + " expected: " + expected + " actual: " + actual + "\n");
			}
		} else if ((expected.getClass().getPackage() != null && expected.getClass().getPackage().getName().startsWith("java"))) {
			if (expected instanceof List) {
				@SuppressWarnings("rawtypes")
				List expectedList = (List) expected;
				@SuppressWarnings("rawtypes")
				List actualList = (List) actual;
				if (expectedList.size() != actualList.size()) {
					message.append("invalid list size. " + fieldName + " expected: " + expectedList.size() + " actual: " + actualList.size() + "\n");
				} else {
					for (int i = 0; i < expectedList.size(); i++) {
						assertElements(message, fieldName + " - " + i, expectedList.get(i), actualList.get(i));
					}
				}
			} else {
				if (!expected.equals(actual)) {
					message.append(fieldName + " expected: " + expected + " actual: " + actual + "\n");
				}
			}
		} else if (expected.getClass().isArray()) {
			int expectedLength = Array.getLength(expected);
			int actualLength = Array.getLength(actual);
			if (expectedLength != actualLength) {
				message.append("invalid array size. " + fieldName + " expected: " + expectedLength + " actual: " + actualLength + "\n");
			} else {
				for (int i = 0; i < expectedLength; i++) {
					Object expectedValue = Array.get(expected, i);
					Object actualValue = Array.get(actual, i);
					assertElements(message, fieldName + " - " + i, expectedValue, actualValue);
				}
			}
		} else {
			Method[] methods = expected.getClass().getDeclaredMethods();
			for (Method m : methods) {
				if (!Modifier.isPublic(m.getModifiers())) {
					continue;
				}
				String curField;
				if (m.getName().startsWith("get")) {
					curField = m.getName().substring(3);
				} else if (m.getName().startsWith("is")) {
					curField = m.getName().substring(2);
				} else {
					continue;
				}
				// check if field marked as transient
				// do not compare such fields
				Field field;
				try {
					field = expected.getClass().getDeclaredField(Character.toLowerCase(curField.charAt(0)) + curField.substring(1));
					if (Modifier.isTransient(field.getModifiers())) {
						continue;
					}
				} catch (Exception e1) {
					// ignore
				}
				try {
					Object actualValue = m.invoke(actual);
					Object expectedValue = m.invoke(expected);
					assertElements(message, curField, expectedValue, actualValue);
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
			}
		}
	}

}
