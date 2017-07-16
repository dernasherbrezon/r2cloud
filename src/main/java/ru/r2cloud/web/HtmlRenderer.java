package ru.r2cloud.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Properties;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

class HtmlRenderer {

	private String basepath;

	HtmlRenderer(Properties props) {
		basepath = props.getProperty("server.ftl.location");
	}

	Response render(String page, Map<String, Object> model) {
		JtwigTemplate template = JtwigTemplate.classpathTemplate(basepath + "/" + page);
		JtwigModel wigModel = JtwigModel.newModel(model);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		template.render(wigModel, baos);
		byte[] bytes = baos.toByteArray();
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, new ByteArrayInputStream(bytes), bytes.length);
	}
}
