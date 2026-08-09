package ru.r2cloud;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.r2cloud.satellite.ProcessFactoryMock;
import ru.r2cloud.satellite.ProcessWrapperMock;

public class ApplicationCheckerTest {

	private ApplicationChecker checker;
	private ProcessFactoryMock processFactory;
	private ProcessWrapperMock appMock;
	private String appPath = UUID.randomUUID().toString();

	@Test
	public void testSuccess() {
		assertTrue(checker.checkApplication("testApp", appPath));
		appMock.setStatusCode(1);
		assertFalse(checker.checkApplication("testApp", appPath));
	}

	@Before
	public void start() {
		Map<String, ProcessWrapperMock> mocks = new HashMap<>();
		appMock = new ProcessWrapperMock(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new ByteArrayInputStream(new byte[0]), 0, false);
		mocks.put(appPath, appMock);
		processFactory = new ProcessFactoryMock(mocks, UUID.randomUUID().toString());

		checker = new ApplicationChecker(processFactory);
	}

}
