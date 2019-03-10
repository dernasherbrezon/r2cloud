package ru.r2cloud.web.api;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.Authenticator;
import ru.r2cloud.web.BadRequest;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.ValidationResult;
import ru.r2cloud.web.WebServer;

public class AccessToken extends AbstractHttpController {

	private final Authenticator auth;

	public AccessToken(Authenticator auth) {
		this.auth = auth;
	}

	@Override
	public ModelAndView doPost(IHTTPSession session) {
		JsonValue request = Json.parse(WebServer.getRequestBody(session));
		if (!request.isObject()) {
			return new BadRequest("expected object");
		}
		String username = WebServer.getString(request, "username");
		String password = WebServer.getString(request, "password");
		return doLogin(auth, username, password);
	}

	public static ModelAndView doLogin(Authenticator auth, String username, String password) {
		String token = auth.authenticate(username, password);
		ModelAndView result = new ModelAndView();
		if (token == null) {
			result.setData(new ValidationResult("Invalid login or password").toJson());
			result.setStatus(Response.Status.UNAUTHORIZED);
		} else {
			JsonObject data = Json.object();
			data.add("access_token", token);
			data.add("token_type", "bearer");
			data.add("expires_in", auth.getMaxAgeMillis() / 1000);
			result.setData(data.toString());
		}
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/accessToken";
	}

}
