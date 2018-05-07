package ru.r2cloud.web.api;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.jradio.sink.Spectogram;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.satellite.ObservationResultDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.InternalServerError;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.WebServer;

public class WeatherSpectrogram extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(WeatherSpectrogram.class);

	private final ObservationResultDao dao;
	private final Spectogram spectogram = new Spectogram(2, 1024);

	public WeatherSpectrogram(ObservationResultDao dao) {
		this.dao = dao;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		String satelliteId = ((JsonObject) request).getString("satelliteId", null);
		String id = ((JsonObject) request).getString("id", null);
		if (id == null || satelliteId == null) {
			LOG.info("missing parameters");
			return new BadRequest("missing parameters");
		}
		ObservationResult observation = dao.find(satelliteId, id);
		if (observation == null) {
			LOG.info("not found: " + satelliteId + " id: " + id);
			return new NotFound();
		}

		if (observation.getWavPath() == null || !observation.getWavPath().exists()) {
			LOG.info("wav file not found");
			return new NotFound();
		}

		try (InputStream is = new BufferedInputStream(new FileInputStream(observation.getWavPath()))) {
			WavFileSource source = new WavFileSource(is);
			BufferedImage image = spectogram.process(source);
			File tmp = File.createTempFile(observation.getId() + "-", "-spectogram.png");
			ImageIO.write(image, "png", tmp);
			if (!dao.saveSpectogram(satelliteId, id, tmp)) {
				LOG.info("unable to save spectogram");
				return new InternalServerError();
			}
		} catch (Exception e) {
			LOG.error("unable to create waterfall", e);
			return new InternalServerError();
		}
		observation = dao.find(satelliteId, id);
		JsonObject entity = new JsonObject();
		entity.add("spectogramURL", observation.getSpectogramURL());
		ModelAndView result = new ModelAndView();
		result.setData(entity.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/weather/spectogram";
	}
}
