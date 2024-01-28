package ru.r2cloud.web.api.observation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import ru.r2cloud.model.Observation;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.NotFound;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;
import ru.r2cloud.web.api.Messages;

public class ObservationSigMfData extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(ObservationSigMfData.class);

	private final IObservationDao resultDao;
	private final Configuration config;
	private final SignedURL signed;

	public ObservationSigMfData(Configuration config, IObservationDao resultDao, SignedURL signed) {
		this.config = config;
		this.resultDao = resultDao;
		this.signed = signed;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ValidationResult errors = new ValidationResult();
		String id = WebServer.getParameter(session, "id");
		if (id == null) {
			errors.put("id", Messages.CANNOT_BE_EMPTY);
		}
		String satelliteId = WebServer.getParameter(session, "satelliteId");
		if (satelliteId == null) {
			errors.put("satelliteId", Messages.CANNOT_BE_EMPTY);
		}

		if (!errors.isEmpty()) {
			return new BadRequest(errors);
		}

		String uri = session.getUri() + "?satelliteId=" + satelliteId + "&id=" + id;
		if (!signed.validate(uri, WebServer.getParameters(session))) {
			return new ModelAndView(NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "invalid signed url"));
		}

		Observation entity = resultDao.find(satelliteId, id);
		if (entity == null || !entity.getRawPath().exists()) {
			LOG.info("not found: {} id: {}", satelliteId, id);
			return new NotFound();
		}

		ModelAndView result = new ModelAndView();
		try {
			SimpleDateFormat dateFormat = createParser();
			Long ifModifiedSince = getIfModifiedSince(session);
			Response response;
			if (ifModifiedSince != null && ifModifiedSince >= entity.getRawPath().lastModified() / 1000) {
				response = NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.NOT_MODIFIED, "application/octet-stream", null);
			} else {
				Long totalBytes = Util.readTotalBytes(entity.getRawPath().toPath());
				InputStream is = new BufferedInputStream(new FileInputStream(entity.getRawPath()));
				if (entity.getRawPath().toString().endsWith(".gz")) {
					is = new GZIPInputStream(is);
				}
				response = NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.OK, "application/octet-stream", is, totalBytes);
				// convert to seconds
				response.addHeader("Cache-Control", "private, max-age=" + ((int) (config.getLong("server.static.signed.validMillis") / 1000)));
			}
			response.addHeader("Content-Disposition", "attachment; filename=r2cloud-" + satelliteId + "-" + id + ".sigmf-meta");
			response.addHeader("Last-Modified", dateFormat.format(new Date(entity.getRawPath().lastModified())));
			result.setRaw(response);
		} catch (IOException e) {
			result.setRaw(NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "unable to find"));
		}
		return result;

	}

	private static Long getIfModifiedSince(IHTTPSession session) {
		String ifModifiedSince = session.getHeaders().get("if-modified-since");
		if (ifModifiedSince == null) {
			return null;
		}
		try {
			return createParser().parse(ifModifiedSince).getTime();
		} catch (Exception e) {
			return null;
		}
	}

	private static SimpleDateFormat createParser() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormat;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/static/observation/sigmf/data";
	}

}
