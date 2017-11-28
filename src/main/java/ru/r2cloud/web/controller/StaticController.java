package ru.r2cloud.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class StaticController {

	private final File basePath;
	private final String canonicalBasePath;
	private final Map<String, String> mimeTypes = new HashMap<String, String>();

	public StaticController(Configuration config) {
		this.basePath = Util.initDirectory(config.getProperty("server.static.location"));
		try {
			this.canonicalBasePath = this.basePath.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException("unable to get canonical path", e);
		}
		mimeTypes.put("rrd", "application/octet-stream");
	}

	public Response doGet(IHTTPSession session) {
		String uri = session.getUri();
		String path = uri.substring(getRequestMappingURL().length());
		File result = new File(basePath, path);
		String requestCanonicalPath;
		try {
			requestCanonicalPath = result.getCanonicalPath();
		} catch (IOException e1) {
			return new503ErrorResponse();
		}
		//prevent escape from configured base directory
		if (!requestCanonicalPath.startsWith(canonicalBasePath) || !result.exists()) {
			return NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "not found");
		}
		try {
			return NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.OK, getMimeType(uri), new FileInputStream(result), result.length());
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

	public String getRequestMappingURL() {
		return "/api/v1/admin/static/";
	}

}
