package ru.r2cloud.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;

import fi.iki.elonen.NanoHTTPD;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.web.api.StaticController;

public class WebServer extends NanoHTTPD {

	private static final Logger LOG = LoggerFactory.getLogger(WebServer.class);
	private static final String ALLOW_HEADERS = "Authorization, Content-Type";

	private final GsonRenderer jsonRenderer;
	private final Map<String, HttpContoller> controllers;
	private final Authenticator auth;

	private final StaticController staticController;

	public WebServer(Configuration props, Map<String, HttpContoller> controllers, Authenticator auth, SignedURL signed) {
		super(props.getProperty("server.hostname"), props.getInteger("server.port"));
		jsonRenderer = new GsonRenderer();
		this.auth = auth;
		this.controllers = controllers;

		staticController = new StaticController(props, signed);

	}

	@Override
	public void start() {
		try {
			start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
			LOG.info("webserver is listening on {}:{}", getHostname(), getListeningPort());
		} catch (IOException e) {
			throw new RuntimeException("unable to start", e);
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		if (session.getMethod().equals(Method.OPTIONS)) {
			Response result = NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", null);
			setupCorsHeaders(result);
			return result;
		}
		if (session.getUri().startsWith(staticController.getRequestMappingURL())) {
			return staticController.doGet(session);
		}
		if (isAuthenticationRequired(session) && !auth.isAuthenticated(session)) {
			Response result = NanoHTTPD.newFixedLengthResponse(Response.Status.UNAUTHORIZED, MimeType.JSON.getType(), "{}");
			setupCorsHeaders(result);
			return result;
		}
		HttpContoller controller = controllers.get(session.getUri());
		ModelAndView model = null;
		if (controller == null) {
			model = new ModelAndView();
			model.setStatus(Response.Status.NOT_FOUND);
		} else {
			try {
				switch (session.getMethod()) {
				case GET:
					model = controller.doGet(session);
					break;
				case POST:
					model = processPost(session, controller);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				LOG.error("unable to handle request", e);
				model = new InternalServerError();
			}
		}

		if (model == null) {
			model = new ModelAndView();
			model.setStatus(Response.Status.INTERNAL_ERROR);
		}
		Response result;
		if (model.getRaw() != null) {
			result = model.getRaw();
		} else {
			result = jsonRenderer.render(model);
			if (model.getStatus() != null) {
				result.setStatus(model.getStatus());
			}
			if (model.getHeaders() != null) {
				for (Entry<String, String> cur : model.getHeaders().entrySet()) {
					result.addHeader(cur.getKey(), cur.getValue());
				}
			}
		}
		setupCorsHeaders(result);
		return result;
	}

	private static ModelAndView processPost(IHTTPSession session, HttpContoller controller) {
		ModelAndView model;
		JsonValue request;
		try {
			request = Json.parse(WebServer.getRequestBody(session));
			if (!request.isObject()) {
				model = new BadRequest("expected object");
			} else {
				model = controller.doPost(request.asObject());
			}
		} catch (ParseException e) {
			model = new BadRequest("expected json object");
		}
		return model;
	}

	private static void setupCorsHeaders(Response result) {
		result.addHeader("Access-Control-Allow-Origin", "*");
		result.addHeader("Access-Control-Max-Age", "1728000");
		result.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
		result.addHeader("Access-Control-Allow-Headers", ALLOW_HEADERS);
		result.addHeader("Access-Control-Expose-Headers", ALLOW_HEADERS);
	}

	public boolean isAuthenticationRequired(IHTTPSession session) {
		return session.getUri().startsWith("/api/v1/admin/");
	}

	public static String getParameter(IHTTPSession session, String name) {
		Map<String, List<String>> parameters = getParameters(session);
		List<String> values = parameters.get(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.get(0);
	}

	public static Map<String, List<String>> getParameters(IHTTPSession session) {
		Map<String, List<String>> parameters = session.getParameters();
		if (parameters.isEmpty()) {
			try {
				session.parseBody(new HashMap<>());
				parameters = session.getParameters();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (ResponseException e) {
				throw new RuntimeException(e);
			}
		}
		return parameters;
	}

	public static String getRequestBody(IHTTPSession session) {
		final HashMap<String, String> map = new HashMap<>();
		try {
			session.parseBody(map);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return map.get("postData");
	}

	public static String getString(JsonValue value, String name) {
		JsonValue field = ((JsonObject) value).get(name);
		if (field == null || field.isNull()) {
			return null;
		}
		if (!field.isString()) {
			return null;
		}
		return field.asString();
	}

	public static Double getDouble(JsonValue value, String name) {
		JsonValue result = ((JsonObject) value).get(name);
		if (result == null || result.isNull()) {
			return null;
		}
		return result.asDouble();
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

	public static Integer getInteger(JsonValue value, String name) {
		JsonValue result = ((JsonObject) value).get(name);
		if (result == null || result.isNull()) {
			return null;
		}
		if (result.isString()) {
			return Integer.parseInt(result.asString());
		}
		if (!result.isNumber()) {
			throw new NumberFormatException();
		}
		return result.asInt();
	}

	public static Long getLong(JsonValue value, String name) {
		JsonValue result = ((JsonObject) value).get(name);
		if (result == null || result.isNull()) {
			return null;
		}
		if (result.isString()) {
			return Long.parseLong(result.asString());
		}
		if (!result.isNumber()) {
			throw new NumberFormatException();
		}
		return result.asLong();
	}

	public static boolean getBoolean(JsonValue value, String name) {
		JsonValue result = ((JsonObject) value).get(name);
		if (result == null || result.isNull()) {
			return false;
		}
		return result.asBoolean();
	}
}
