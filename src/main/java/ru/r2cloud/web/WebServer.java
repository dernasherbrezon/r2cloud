package ru.r2cloud.web;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

	private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

	private final PageRenderer pageRenderer;
	private final Map<String, HttpContoller> controllers;

	public WebServer(String hostname, int port, Map<String, HttpContoller> controllers) {
		super(hostname, port);
		pageRenderer = new PageRenderer();
		this.controllers = controllers;
	}

	@Override
	public void start() {
		try {
			start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			LOG.info("webserver is listening on " + getHostname() + ":" + getListeningPort());
		} catch (IOException e) {
			throw new RuntimeException("unable to start", e);
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		HttpContoller controller = controllers.get(session.getUri());
		if (controller == null) {
			// FIXME return
			return null;
		}
		ModelAndView model = controller.httpGet(session);
		if (model == null) {
			model = new ModelAndView();
		}
		try {
			return pageRenderer.render(model.getView(), model);
		} catch (IOException e) {
			// FIXME logging and 503 status
			return null;
		}
	}

}
