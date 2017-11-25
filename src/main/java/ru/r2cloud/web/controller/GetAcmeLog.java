package ru.r2cloud.web.controller;

import java.util.List;

import ru.r2cloud.ssl.AcmeClient;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.MimeType;
import ru.r2cloud.web.ModelAndView;
import ru.r2cloud.web.WebServer;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class GetAcmeLog extends AbstractHttpController {

	private final AcmeClient client;

	public GetAcmeLog(AcmeClient client) {
		this.client = client;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		//FIXME
		//result.setType(MimeType.JSON);
		if (client.isRunning()) {
			result.setStatus(Response.Status.PARTIAL_CONTENT);
		}
		JsonArray array = (JsonArray) Json.array();
		int index = WebServer.getInteger(session, "index");
		List<String> messages = client.getMessages();
		for (int i = index; i < messages.size(); i++) {
			array.add(messages.get(i));
		}
		result.put("entity", array.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/config/ssl/log.json";
	}
}