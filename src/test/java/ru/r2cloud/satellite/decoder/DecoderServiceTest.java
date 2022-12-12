package ru.r2cloud.satellite.decoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ru.r2cloud.it.util.BaseTest;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationStatus;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.DefaultClock;
import ru.r2cloud.util.ThreadPoolFactory;

public class DecoderServiceTest {
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private DecoderService service;
	private IObservationDao dao;
	private ThreadPoolFactory factory;
	private ScheduledExecutorService executor;

	@Test
	public void testScheduleTwice() throws Exception {
		Observation observation = new Observation();
		observation.setId(UUID.randomUUID().toString());
		observation.setStatus(ObservationStatus.RECEIVED);
		observation.setRawPath(tempFolder.getRoot());
		when(dao.findAll()).thenReturn(Collections.singletonList(observation));
		
		service.retryObservations();
		service.retryObservations();
		
		verify(executor, times(1)).execute(any());
	}
	
	@Before
	public void start() throws Exception {
		File userSettingsLocation = new File(tempFolder.getRoot(), ".r2cloud-" + UUID.randomUUID().toString());
		Configuration config;
		try (InputStream is = BaseTest.class.getClassLoader().getResourceAsStream("config-dev.properties")) {
			config = new Configuration(is, userSettingsLocation.getAbsolutePath(), "config-common-test.properties", FileSystems.getDefault());
		}
		factory = mock(ThreadPoolFactory.class);
		executor = mock(ScheduledExecutorService.class);
		when(factory.newScheduledThreadPool(anyInt(), any())).thenReturn(executor);

		dao = mock(IObservationDao.class);

		service = new DecoderService(config, null, dao, null, factory, new Metrics(config, new DefaultClock()), null);
		service.start();
	}
	
}
