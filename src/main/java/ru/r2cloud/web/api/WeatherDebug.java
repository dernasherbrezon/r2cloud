package ru.r2cloud.web.api;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.jradio.sink.Waterfall;
import ru.r2cloud.jradio.source.WavFileSource;
import ru.r2cloud.model.ObservationResult;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.Success;
import ru.r2cloud.web.WebServer;

public class WeatherDebug extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(WeatherDebug.class);

	private final Configuration config;
	private final SatelliteDao dao;
	private final Waterfall waterfall = new Waterfall(2, 1024);

	public WeatherDebug(Configuration config, SatelliteDao dao) {
		this.config = config;
		this.dao = dao;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		String id = ((JsonObject) request).getString("id", null);
		Long date = WebServer.getLong(request, "date");
		List<Satellite> supported = dao.findSupported();
		for (Satellite cur : supported) {
			if (!cur.getId().equals(id)) {
				continue;
			}
			List<ObservationResult> observations = dao.findWeatherObservations(cur);
			for (ObservationResult curResult : observations) {
				if (!date.equals(curResult.getDate().getTime())) {
					continue;
				}
				// FIXME move to satellite dao
				File basepath = new File(Util.initDirectory(config.getProperty("satellites.basepath.location")), cur.getId() + File.separator + "data" + File.separator + date);
				File wavPath = new File(basepath, "output.wav");
				if (!wavPath.exists()) {
					LOG.error("wav file not found: " + wavPath);
					continue;
				}
				try (InputStream is = new BufferedInputStream(new FileInputStream(wavPath))) {
					WavFileSource source = new WavFileSource(is);
					BufferedImage image = waterfall.process(source);
					ImageIO.write(image, "png", new File(basepath, "waterfall.png"));
				} catch (Exception e) {
					LOG.error("unable to create waterfall", e);
				}
			}
		}
		return new Success();
	}

	// FIXME load/reload by id/date to optimize speed 
	@Override
	public ModelAndView doGet(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		String id = ((JsonObject) request).getString("id", null);
		Long date = WebServer.getLong(request, "date");
		ObservationResult entity = dao.findWeatherObservation(id, date);
		ModelAndView result = new ModelAndView();
		if (entity != null) {
			result.setData(convert(entity).toString());
		} else {
			result.setData("{}");
		}
		return result;
	}

	private static JsonObject convert(ObservationResult curObservation) {
		JsonObject curObservationObject = new JsonObject();
		curObservationObject.add("date", curObservation.getDate().getTime());
		curObservationObject.add("aPath", curObservation.getaPath());
		curObservationObject.add("bPath", curObservation.getbPath());
		curObservationObject.add("waterfall", curObservation.getWaterfall());
		return curObservationObject;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/weather/debug";
	}
}
