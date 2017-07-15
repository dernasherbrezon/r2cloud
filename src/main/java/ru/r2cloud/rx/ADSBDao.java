package ru.r2cloud.rx;

import java.util.HashMap;
import java.util.Map;

import org.opensky.libadsb.tools;
import org.opensky.libadsb.msgs.ModeSReply;

public class ADSBDao {

	private final Map<String, ModeSReply> latestMessages = new HashMap<String, ModeSReply>();

	public void save(ModeSReply reply) {
		latestMessages.put(tools.toHexString(reply.getIcao24()), reply);
	}

	public Map<String, ModeSReply> getLatestMessages() {
		return latestMessages;
	}

}
