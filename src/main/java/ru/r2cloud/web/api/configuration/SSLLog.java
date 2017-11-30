package ru.r2cloud.web.api.configuration;

import java.util.List;

import com.eclipsesource.json.JsonArray;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.WebServer;

public class SSLLog extends AbstractHttpController {

	private final AcmeClient client;

	public SSLLog(AcmeClient client) {
		this.client = client;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		if (client.isRunning()) {
			result.setStatus(Response.Status.PARTIAL_CONTENT);
		}
		JsonArray array = new JsonArray();
		int index = WebServer.getInteger(session, "index");
		List<String> messages = client.getMessages();
		for (int i = index; i < messages.size(); i++) {
			array.add(messages.get(i));
		}
		result.setData(array.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/config/ssl/log";
	}
}