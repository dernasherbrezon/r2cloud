package ru.r2cloud.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import ru.r2cloud.util.Configuration;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

class HtmlRenderer {

	private final String basepath;
	private final boolean useCache;
	private final Map<String, JtwigTemplate> cache = new ConcurrentHashMap<String, JtwigTemplate>();

	HtmlRenderer(Configuration props) {
		basepath = props.getProperty("server.ftl.location");
		useCache = props.getBoolean("server.ftl.cache");
	}

	Response render(String page, Map<String, Object> model) {
		JtwigTemplate template = cache.get(page);
		if (!useCache || template == null) {
			// double initialization is possible here
			// however it's that not critical
			template = JtwigTemplate.fileTemplate(new File(basepath, page));
			cache.put(page, template);
		}
		JtwigModel wigModel = JtwigModel.newModel(model);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		template.render(wigModel, baos);
		byte[] bytes = baos.toByteArray();
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, new ByteArrayInputStream(bytes), bytes.length);
	}
}
