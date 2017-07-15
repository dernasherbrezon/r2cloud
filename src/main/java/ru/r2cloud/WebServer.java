package ru.r2cloud;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.r2cloud.controller.Home;
import fi.iki.elonen.NanoHTTPD;

class WebServer extends NanoHTTPD {
	
	private PageRenderer pageRenderer;
	
	private Map<String, HttpContoller> controllers = new HashMap<String, HttpContoller>();

	public WebServer(String hostname, int port) {
		super(hostname, port);
		pageRenderer = new PageRenderer();
		index(new Home());
	}

	public void start() {
		try {
			start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		} catch (IOException e) {
			throw new RuntimeException("unable to start", e);
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		HttpContoller controller = controllers.get(session.getUri());
		if( controller == null ) {
			//FIXME return 
			return null;
		}
		ModelAndView model = controller.httpGet(session);
		if( model == null ) {
			model = new ModelAndView();
		}
		try {
			return pageRenderer.render(model.getView(), model);
		} catch (IOException e) {
			//FIXME logging and 503 status
			return null;
		}
//		String msg = "<html><body><h1>Hello server</h1>\n";
//		Map<String, String> parms = session.getParms();
//		if (parms.get("username") == null) {
//			msg += "<form action='?' method='get'>\n  <p>Your name: <input type='text' name='username'></p>\n" + "</form>\n";
//		} else {
//			msg += "<p>Hello, " + parms.get("username") + "!</p>";
//		}
//		return newFixedLengthResponse(msg + "</body></html>\n");
	}
	
	private void index(HttpContoller controller) {
		controllers.put(controller.getRequestMappingURL(), controller);
	}
}
