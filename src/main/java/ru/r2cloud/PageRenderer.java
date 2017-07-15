package ru.r2cloud;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

class PageRenderer {

	private final Configuration cfg = new Configuration();

	PageRenderer() {
		try {
			cfg.setDirectoryForTemplateLoading(new File("src/main/resources/ftl"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}

	Response render(String page, Map<String, Object> model) throws IOException {
		Template temp = cfg.getTemplate(page);
		StringWriter w = new StringWriter();
		try {
			temp.process(model, w);
		} catch (TemplateException e) {
			throw new IOException("invalid template: " + page, e);
		}
		return NanoHTTPD.newFixedLengthResponse(w.toString());
	}
}
