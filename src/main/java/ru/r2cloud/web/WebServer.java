package ru.r2cloud.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.web.controller.StaticController;
import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {

	private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

	private final HtmlRenderer pageRenderer;
	private final GsonRenderer jsonRenderer;
	private final Map<String, HttpContoller> controllers;
	private final Authenticator auth;
	private final Set<String> urlsAccessibleOnFirstStart = new HashSet<>();

	private final StaticController staticController;

	public WebServer(Configuration props, Map<String, HttpContoller> controllers, Authenticator auth) {
		super(props.getProperty("server.hostname"), Integer.valueOf(props.getProperty("server.port")));
		pageRenderer = new HtmlRenderer(props);
		jsonRenderer = new GsonRenderer();
		this.auth = auth;
		this.controllers = controllers;

		staticController = new StaticController(props);

		urlsAccessibleOnFirstStart.add("/setup");
		urlsAccessibleOnFirstStart.add("/doSetup");
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
		if (auth.isFirstStart() && !urlsAccessibleOnFirstStart.contains(session.getUri())) {
			return newRedirectResponse("/setup");
		} else if (!auth.isFirstStart() && urlsAccessibleOnFirstStart.contains(session.getUri())) {
			return newRedirectResponse("/");
		}
		if (auth.isAuthenticationRequired(session) && !auth.isAuthenticated(session)) {
			return newRedirectResponse("/");
		}
		if (session.getUri().startsWith(staticController.getRequestMappingURL())) {
			return staticController.doGet(session);
		}
		HttpContoller controller = controllers.get(session.getUri());
		ModelAndView model = null;
		if (controller == null) {
			model = new ModelAndView("404");
			model.setStatus(Response.Status.NOT_FOUND);
		} else {
			switch (session.getMethod()) {
			case GET:
				model = controller.doGet(session);
				break;
			case POST:
				model = controller.doPost(session);
				break;
			default:
				break;
			}
		}

		if (model == null) {
			model = new ModelAndView("503");
			model.setStatus(Response.Status.BAD_REQUEST);
		}
		switch (model.getType()) {
		case HTML:
			Response result;
			if (model.getView() != null) {
				result = pageRenderer.render(model.getView() + ".jtwig", model);
			} else {
				result = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, MIME_HTML, "");
			}
			if (model.getStatus() != null) {
				result.setStatus(model.getStatus());
			}
			if (model.getHeaders() != null) {
				for (Entry<String, String> cur : model.getHeaders().entrySet()) {
					result.addHeader(cur.getKey(), cur.getValue());
				}
			}
			return result;
		case JSON:
			return jsonRenderer.render(model);
		default:
			throw new IllegalArgumentException("unsupported mime type: " + model.getType());
		}
	}

	public static Response newRedirectResponse(String location) {
		@SuppressWarnings("deprecation")
		Response r = NanoHTTPD.newFixedLengthResponse(Response.Status.FOUND, MIME_HTML, "");
		r.addHeader("Location", location);
		return r;
	}

	public static String getParameter(IHTTPSession session, String name) {
		Map<String, List<String>> parameters = session.getParameters();
		if (parameters.isEmpty()) {
			Map<String, String> files = new HashMap<String, String>();
			try {
				session.parseBody(files);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ResponseException e) {
				throw new RuntimeException(e);
			}
		}
		List<String> values = parameters.get(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.get(0);
	}

}
