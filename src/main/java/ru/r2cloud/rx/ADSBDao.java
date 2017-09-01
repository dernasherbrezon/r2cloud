package ru.r2cloud.rx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.opensky.libadsb.Position;
import org.opensky.libadsb.PositionDecoder;
import org.opensky.libadsb.tools;
import org.opensky.libadsb.msgs.AirbornePositionMsg;
import org.opensky.libadsb.msgs.ModeSReply;
import org.opensky.libadsb.msgs.ModeSReply.subtype;

import ru.r2cloud.model.Airplane;
import ru.r2cloud.uitl.Configuration;
import ru.r2cloud.uitl.NamingThreadFactory;

public class ADSBDao {

	private static final Logger LOG = Logger.getLogger(ADSBDao.class.getName());

	private final Map<String, Airplane> latestMessages = new HashMap<String, Airplane>();
	private final Configuration props;

	private ScheduledExecutorService reaper;

	public ADSBDao(Configuration props) {
		this.props = props;
	}

	public void start() {
		long cleanupPeriodMs = props.getLong("rx.adsb.cleanupPeriodMs");

		reaper = Executors.newScheduledThreadPool(1, new NamingThreadFactory("adsb-data-reaper"));
		reaper.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				long expireAt = System.currentTimeMillis() - cleanupPeriodMs;
				synchronized (latestMessages) {
					for (Iterator<Map.Entry<String, Airplane>> it = latestMessages.entrySet().iterator(); it.hasNext();) {
						Map.Entry<String, Airplane> entry = it.next();
						if (entry.getValue().getLastUpdatedAt() < expireAt) {
							LOG.info("expiring data: " + entry.getKey());
							it.remove();
						}
					}
				}
			}
		}, cleanupPeriodMs, cleanupPeriodMs, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (reaper != null) {
			reaper.shutdown();
		}
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

		// FIXME rotate

		plane.setLastUpdatedAt(System.currentTimeMillis());
		plane.getPositions().add(position);
	}

	public Collection<Airplane> getAirplanes() {
		synchronized (latestMessages) {
			return latestMessages.values();
//			List<Airplane> result = new ArrayList<Airplane>();
//			result.add(new Airplane());
//			return result;
		}
	}
}
