package ru.r2cloud.rx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opensky.libadsb.Position;
import org.opensky.libadsb.PositionDecoder;
import org.opensky.libadsb.tools;
import org.opensky.libadsb.msgs.AirbornePositionMsg;
import org.opensky.libadsb.msgs.ModeSReply;
import org.opensky.libadsb.msgs.ModeSReply.subtype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.Airplane;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.NamingThreadFactory;
import ru.r2cloud.util.SafeRunnable;
import ru.r2cloud.util.Util;

public class ADSBDao {

	private static final Logger LOG = LoggerFactory.getLogger(ADSBDao.class);

	private final Map<String, Airplane> latestMessages = new HashMap<String, Airplane>();
	private final Configuration props;
	private final boolean enabled;

	private ScheduledExecutorService reaper;

	public ADSBDao(Configuration props) {
		this.props = props;
		this.enabled = props.getBoolean("rx.adsb.enabled");
	}

	public synchronized void start() {
		if (!enabled) {
			return;
		}
		if (reaper != null) {
			return;
		}
		long cleanupPeriodMs = props.getLong("rx.adsb.cleanupPeriodMs");

		reaper = Executors.newScheduledThreadPool(1, new NamingThreadFactory("adsb-data-reaper"));
		reaper.scheduleAtFixedRate(new SafeRunnable() {

			@Override
			public void doRun() {
				long expireAt = System.currentTimeMillis() - cleanupPeriodMs;
				synchronized (latestMessages) {
					for (Iterator<Map.Entry<String, Airplane>> it = latestMessages.entrySet().iterator(); it.hasNext();) {
						Map.Entry<String, Airplane> entry = it.next();
						if (entry.getValue().getLastUpdatedAt() < expireAt) {
							if (LOG.isDebugEnabled()) {
								LOG.debug("expiring data: " + entry.getKey());
							}
							it.remove();
						}
					}
				}
			}
		}, cleanupPeriodMs, cleanupPeriodMs, TimeUnit.MILLISECONDS);
	}

	public synchronized void stop() {
		if (!enabled) {
			return;
		}
		Util.shutdown(reaper, props.getThreadPoolShutdownMillis());
		reaper = null;
	}

	public void save(ModeSReply reply) {
		if (!reply.getType().equals(subtype.ADSB_AIRBORN_POSITION)) {
			return;
		}
		AirbornePositionMsg airpos = (AirbornePositionMsg) reply;

		Position position = null;
		Airplane plane = null;
		String icao24 = tools.toHexString(reply.getIcao24());
		synchronized (latestMessages) {
			plane = latestMessages.get(icao24);
			if (plane == null) {
				PositionDecoder dec = new PositionDecoder();
				position = dec.decodePosition(System.currentTimeMillis() / 1000, airpos);

				plane = new Airplane();
				plane.setPositions(new CopyOnWriteArrayList<Position>());
				plane.setDecoder(dec);
				plane.setIcao24(icao24);

				latestMessages.put(icao24, plane);
			} else {
				airpos.setNICSupplementA(plane.getDecoder().getNICSupplementA());
				position = plane.getDecoder().decodePosition(System.currentTimeMillis() / 1000, airpos);
			}
		}

		if (position == null) {
			return;
		}

		plane.setLastUpdatedAt(System.currentTimeMillis());
		plane.getPositions().add(position);
	}

	public Collection<Airplane> getAirplanes() {
		synchronized (latestMessages) {
			return latestMessages.values();
		}
	}
}
