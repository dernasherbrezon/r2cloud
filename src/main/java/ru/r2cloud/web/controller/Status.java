package ru.r2cloud.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ru.r2cloud.metrics.FormattedCounter;
import ru.r2cloud.metrics.FormattedGauge;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.model.MetricInfo;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

import com.codahale.metrics.Metric;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class Status extends AbstractHttpController {

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		long end = System.currentTimeMillis();
		long start = end - 24 * 60 * 60 * 1000; // last 1 day
		ModelAndView result = new ModelAndView("status");
		result.put("start", start);
		result.put("end", end);
		result.put("metrics", convert(Metrics.REGISTRY.getMetrics()));
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/admin/status";
	}

	//workaround for https://github.com/jtwig/jtwig/issues/355
	private static List<MetricInfo> convert(Map<String, Metric> map) {
		List<MetricInfo> result = new ArrayList<MetricInfo>();
		for (Entry<String, Metric> cur : map.entrySet()) {
			MetricInfo curInfo = new MetricInfo();
			curInfo.setName(cur.getKey());
			if (cur.getValue() instanceof FormattedCounter) {
				curInfo.setFormat(((FormattedCounter) cur.getValue()).getFormat());
			}
			if (cur.getValue() instanceof FormattedGauge<?>) {
				curInfo.setFormat(((FormattedGauge<?>) cur.getValue()).getFormat());
			}
			result.add(curInfo);
		}
		return result;
	}

}
