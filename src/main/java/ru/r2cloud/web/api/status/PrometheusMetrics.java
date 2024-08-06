package ru.r2cloud.web.api.status;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.metrics.Metrics;
import ru.r2cloud.util.Util;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.InternalServerError;
import ru.r2cloud.web.ModelAndView;

public class PrometheusMetrics extends AbstractHttpController {

	private static final Logger LOG = LoggerFactory.getLogger(PrometheusMetrics.class);
	private final Metrics metrics;

	public PrometheusMetrics(Metrics metrics) {
		this.metrics = metrics;
	}

	@Override
	public ModelAndView doGet(IHTTPSession session) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		long current = System.currentTimeMillis();
		try (Writer w = new BufferedWriter(new OutputStreamWriter(baos))) {
			for (Entry<String, Metric> cur : metrics.getRegistry().getMetrics().entrySet()) {
				String metricName = cur.getKey();
				if (cur.getValue() instanceof Gauge<?>) {
					w.append("# TYPE ").append(cur.getKey()).append(" gauge\n");
					Gauge<?> metric = (Gauge<?>) cur.getValue();
					w.append(cur.getKey()).append(" ").append(metric.getValue().toString()).append(" ").append(String.valueOf(current)).append("\n");
				}
				if (cur.getValue() instanceof Counter) {
					metricName = metricName + "_total";
					w.append("# TYPE ").append(metricName).append(" counter\n");
					Counter metric = (Counter) cur.getValue();
					w.append(metricName).append(" ").append(String.valueOf(metric.getCount())).append(" ").append(String.valueOf(current)).append("\n");
				}
			}
		} catch (Exception e) {
			LOG.error("unable to process metrics", e);
			return new InternalServerError();
		}
		Util.closeQuietly(baos);
		byte[] data = baos.toByteArray();
		ModelAndView result = new ModelAndView();
		result.setRaw(NanoHTTPD.newFixedLengthResponse(fi.iki.elonen.NanoHTTPD.Response.Status.OK, "text/plain; version=0.0.4; charset=utf-8", new ByteArrayInputStream(data), data.length));
		String acceptEncoding = session.getHeaders().get("accept-encoding");
		boolean useGzip = (acceptEncoding != null && acceptEncoding.contains("gzip"));
		result.getRaw().setGzipEncoding(useGzip);
		return result;
	}

	@Override
	public String getRequestMappingURL() {
		return "/api/v1/metrics";
	}

}
