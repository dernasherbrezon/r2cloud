package ru.r2cloud.web.api.status;

import java.util.Map.Entry;

import com.codahale.metrics.Metric;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.metrics.FormattedCounter;
import ru.r2cloud.metrics.FormattedGauge;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class Metrics extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonArray array = new JsonArray();
		for (Entry<String, Metric> cur : ru.r2cloud.metrics.Metrics.REGISTRY.getMetrics().entrySet()) {
			JsonObject curObject = new JsonObject();
			curObject.add("id", cur.getKey());
			curObject.add("url", "/admin/static/rrd/" + cur.getKey() + ".rrd");
			if (cur.getValue() instanceof FormattedCounter) {
				curObject.add("format", ((FormattedCounter) cur.getValue()).getFormat().toString());
			}
			if (cur.getValue() instanceof FormattedGauge<?>) {
				curObject.add("format", ((FormattedGauge<?>) cur.getValue()).getFormat().toString());
			}
			array.add(curObject);
		}
		result.setData(array.toString());
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/admin/status/metrics";
	}

}
