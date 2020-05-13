package ru.r2cloud.web.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.WebServer;

public class StaticController {

	private final File basePath;
	private final String canonicalBasePath;
	private final Map<String, String> mimeTypes = new HashMap<>();
	private final SignedURL signed;
	private final Configuration config;

	public StaticController(Configuration config, SignedURL signed) {
		this.basePath = Util.initDirectory(config.getProperty("server.static.location"));
		try {
			this.canonicalBasePath = this.basePath.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException("unable to get canonical path", e);
		}
		mimeTypes.put("rrd", "application/octet-stream");
		mimeTypes.put("jpg", "image/jpeg");
		mimeTypes.put("png", "image/png");
		this.signed = signed;
		this.config = config;
	}

	public Response doGet(IHTTPSession session) {
		String uri = session.getUri();
		if (!signed.validate(uri, WebServer.getParameters(session))) {
			return NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT, "invalid signed url");
		}
		String path = uri.substring(getRequestMappingURL().length());
		File file = new File(basePath, path);
		String requestCanonicalPath;
		try {
			requestCanonicalPath = file.getCanonicalPath();
		} catch (IOException e1) {
			return new503ErrorResponse();
		}
		// prevent escape from configured base directory
		if (!requestCanonicalPath.startsWith(canonicalBasePath) || !file.exists()) {
			return NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "not found");
		}
		try {
			SimpleDateFormat dateFormat = createParser();
			Long ifModifiedSince = getIfModifiedSince(session);
			Response response;
			if (ifModifiedSince != null && ifModifiedSince >= file.lastModified() / 1000) {
				response = NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.NOT_MODIFIED, getMimeType(uri), null);
			} else {
				response = NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.OK, getMimeType(uri), new FileInputStream(file), file.length());
				// convert to seconds
				response.addHeader("Cache-Control", "private, max-age=" + ((int) (config.getLong("server.static.signed.validMillis") / 1000)));
			}
			response.addHeader("Last-Modified", dateFormat.format(new Date(file.lastModified())));
			return response;
		} catch (FileNotFoundException e) {
			return new503ErrorResponse();
		}
	}

	private static Response new503ErrorResponse() {
		return NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "unable to find");
	}

	private String getMimeType(String uri) {
		int extension = uri.lastIndexOf('.');
		String result = null;
		if (extension != -1) {
			result = mimeTypes.get(uri.substring(extension + 1));
		}
		if (result != null) {
			return result;
		}
		return "application/octet-stream";
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

	public String getRequestMappingURL() {
		return "/api/v1/admin/static/";
	}

}
