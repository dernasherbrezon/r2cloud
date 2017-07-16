package ru.r2cloud.web;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

	private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

	private final HtmlRenderer pageRenderer;
	private final GsonRenderer jsonRenderer;
	private final Map<String, HttpContoller> controllers;

	public WebServer(Properties props, Map<String, HttpContoller> controllers) {
		super(props.getProperty("server.hostname"), Integer.valueOf(props.getProperty("server.port")));
		pageRenderer = new HtmlRenderer(props);
		jsonRenderer = new GsonRenderer();
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
		switch (model.getType()) {
		case HTML:
			return pageRenderer.render(model.getView() + ".jtwig", model);
		case JSON:
			return jsonRenderer.render(model);
		default:
			throw new IllegalArgumentException("unsupported mime type: " + model.getType());
		}
	}

}
