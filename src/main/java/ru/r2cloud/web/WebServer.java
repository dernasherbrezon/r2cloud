package ru.r2cloud.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.controller.StaticController;

public class WebServer extends NanoHTTPD {

	private static final Logger LOG = LoggerFactory.getLogger(WebServer.class);
	private static final String ALLOW_HEADERS = "Authorization, Content-Type";

	private final GsonRenderer jsonRenderer;
	private final Map<String, HttpContoller> controllers;
	private final Authenticator auth;
	private final Set<String> urlsAccessibleOnFirstStart = new HashSet<>();

	private final StaticController staticController;

	public WebServer(Configuration props, Map<String, HttpContoller> controllers, Authenticator auth) {
		super(props.getProperty("server.hostname"), props.getInteger("server.port"));
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
		if (session.getMethod().equals(Method.OPTIONS)) {
			Response result = NanoHTTPD.newFixedLengthResponse(Response.Status.NO_CONTENT, "text/plain; charset=utf-8", "");
			result.addHeader("Access-Control-Allow-Origin", "*");
			result.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
			result.addHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
			result.addHeader("Access-Control-Expose-Headers", ALLOW_HEADERS);
			return result;
		}
		if (auth.isFirstStart() && !urlsAccessibleOnFirstStart.contains(session.getUri())) {
			return newRedirectResponse("/setup");
		} else if (!auth.isFirstStart() && urlsAccessibleOnFirstStart.contains(session.getUri())) {
			return newRedirectResponse("/");
		}
		if (auth.isAuthenticationRequired(session) && !auth.isAuthenticated(session)) {
			String accept = session.getHeaders().get("accept");
			if (accept != null && accept.contains("application/json")) {
				ModelAndView model = new ModelAndView();
				model.setStatus(Response.Status.UNAUTHORIZED);
				model.put("entity", "{ \"error\": \"UNAUTHORIZED\"}");
				return jsonRenderer.render(model);
			}
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
			try {
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
			} catch (Exception e) {
				LOG.error("unable to handle request", e);
				model = new ModelAndView();
				model.setStatus(Response.Status.INTERNAL_ERROR);
			}
		}

		if (model == null) {
			model = new ModelAndView("503");
			model.setStatus(Response.Status.BAD_REQUEST);
		}
		Response result = jsonRenderer.render(model);
		if (model.getStatus() != null) {
			result.setStatus(model.getStatus());
		}
		if (model.getHeaders() != null) {
			for (Entry<String, String> cur : model.getHeaders().entrySet()) {
				result.addHeader(cur.getKey(), cur.getValue());
			}
		}
		result.addHeader("Access-Control-Allow-Origin", "*");
		result.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		result.addHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
		result.addHeader("Access-Control-Expose-Headers", ALLOW_HEADERS);
		return result;
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
			try {
				session.parseBody(new HashMap<String, String>());
				parameters = session.getParameters();
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

	public static String getRequestBody(IHTTPSession session) {
		final HashMap<String, String> map = new HashMap<String, String>();
		try {
			session.parseBody(map);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return map.get("postData");
	}

	public static Double getDouble(IHTTPSession session, String name) {
		String param = getParameter(session, name);
		if (param == null || param.trim().length() == 0) {
			return null;
		}
		return Double.valueOf(param);
	}

	public static Integer getInteger(IHTTPSession session, String name) {
		String param = getParameter(session, name);
		if (param == null || param.trim().length() == 0) {
			return null;
		}
		return Integer.valueOf(param);
	}

	public static boolean getBoolean(IHTTPSession session, String name) {
		String param = getParameter(session, name);
		if (param == null || param.trim().length() == 0) {
			return false;
		}
		return Boolean.valueOf(param);
	}

}
