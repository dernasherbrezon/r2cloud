package ru.r2cloud.web.api.status;

import java.util.Map.Entry;

import com.codahale.metrics.Metric;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.metrics.FormattedGauge;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.util.SignedURL;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class MetricsController extends AbstractHttpController {

	private final SignedURL signed;
	private final Metrics metrics;

	public MetricsController(SignedURL signed, Metrics metrics) {
		this.signed = signed;
		this.metrics = metrics;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ModelAndView result = new ModelAndView();
		JsonArray array = new JsonArray();
		for (Entry<String, Metric> cur : metrics.getRegistry().getMetrics().entrySet()) {
			JsonObject curObject = new JsonObject();
			curObject.add("id", cur.getKey());
			curObject.add("url", signed.sign("/api/v1/admin/static/rrd/" + cur.getKey() + ".rrd"));
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
