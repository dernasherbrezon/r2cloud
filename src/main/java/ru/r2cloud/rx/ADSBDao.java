package ru.r2cloud.rx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opensky.libadsb.Position;
import org.opensky.libadsb.PositionDecoder;
import org.opensky.libadsb.tools;
import org.opensky.libadsb.msgs.AirbornePositionMsg;
import org.opensky.libadsb.msgs.ModeSReply;
import org.opensky.libadsb.msgs.ModeSReply.subtype;

import ru.r2cloud.model.Airplane;

public class ADSBDao {

	private final Map<String, Airplane> latestMessages = new HashMap<String, Airplane>();

	public void save(ModeSReply reply) {
		if (!reply.getType().equals(subtype.ADSB_AIRBORN_POSITION)) {
			return;
		}
		AirbornePositionMsg airpos = (AirbornePositionMsg) reply;

		String icao24 = tools.toHexString(reply.getIcao24());
		Airplane plane = latestMessages.get(icao24);
		Position position = null;
		if (plane == null) {
			PositionDecoder dec = new PositionDecoder();
			position = dec.decodePosition(System.currentTimeMillis() / 1000, airpos);

			plane = new Airplane();
			plane.setPositions(new ArrayList<Position>());
			plane.setDecoder(dec);
			plane.setIcao24(icao24);

			latestMessages.put(icao24, plane);
		} else {
			airpos.setNICSupplementA(plane.getDecoder().getNICSupplementA());
			position = plane.getDecoder().decodePosition(System.currentTimeMillis() / 1000, airpos);
		}

		if (position == null) {
			return;
		}
		
		//FIXME rotate

		plane.getPositions().add(position);
	}

	public Collection<Airplane> getAirplanes() {
		return latestMessages.values();
	}
}
