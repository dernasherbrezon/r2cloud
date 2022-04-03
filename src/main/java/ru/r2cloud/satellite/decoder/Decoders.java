package ru.r2cloud.satellite.decoder;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.model.DecoderKey;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;

public class Decoders {

	private static final Logger LOG = LoggerFactory.getLogger(Decoders.class);

	private final SatelliteDao satelliteDao;
	private final Map<DecoderKey, Decoder> decoders = new HashMap<>();

	public Decoders(PredictOreKit predict, Configuration props, ProcessFactory processFactory, SatelliteDao satelliteDao) {
		this.satelliteDao = satelliteDao;
		index("25338", "25338-0", new APTDecoder(props, processFactory));
		index("28654", "28654-0", new APTDecoder(props, processFactory));
		index("32789", "32789-0", new DelfiC3Decoder(predict, props));
		index("33591", "33591-0", new APTDecoder(props, processFactory));
		index("39430", "39430-0", new Gomx1Decoder(predict, props));
		index("39444", "39444-0", new Ao73Decoder(predict, props));
		index("40069", "40069-0", new LRPTDecoder(predict, props));
		index("41460", "41460-0", new Aausat4Decoder(predict, props));
		index("42017", "42017-0", new Nayif1Decoder(predict, props));
		index("42784", "42784-0", new PegasusDecoder(predict, props));
		index("42829", "42829-0", new TechnosatDecoder(predict, props));
		index("48900", "48900-0", new TechnosatDecoder(predict, props));
		index("43186", "43186-0", new SnetDecoder(predict, props));
		index("43187", "43187-0", new SnetDecoder(predict, props));
		index("43188", "43188-0", new SnetDecoder(predict, props));
		index("43189", "43189-0", new SnetDecoder(predict, props));
		index("43743", "43743-0", new ReaktorHelloWorldDecoder(predict, props));
		index("43792", "43792-0", new EseoDecoder(predict, props));
		index("43798", "43798-0", new AstrocastDecoder(predict, props));
		index("44083", "44083-0", new AstrocastDecoder(predict, props));
		index("43803", "43803-0", new Jy1satDecoder(predict, props));
		index("43804", "43804-0", new Suomi100Decoder(predict, props));
		index("43881", "43881-0", new Dstar1Decoder(predict, props));
//		decoders.put("44406", new Lucky7Decoder(predict, props));
//		decoders.put("44878", new OpsSatDecoder(predict, props));
//		decoders.put("44885", new Floripasat1Decoder(predict, props));
//		decoders.put("43017", new FoxSlowDecoder<>(predict, props, Fox1BBeacon.class));
//		decoders.put("43770", new FoxSlowDecoder<>(predict, props, Fox1CBeacon.class));
//		decoders.put("43137", new FoxDecoder<>(predict, props, Fox1DBeacon.class));
		index("43855", "43855-0", new ChompttDecoder(predict, props));
//		decoders.put("41789", new Alsat1nDecoder(predict, props));
//		decoders.put("39090", new Strand1Decoder(predict, props));
//		decoders.put("46495", new SalsatDecoder(predict, props));
//		decoders.put("46489", new BpskAx25G3ruhDecoder(predict, props, MeznsatBeacon.class, null));
//		decoders.put("42792", new AfskAx25Decoder(predict, props, Ax25Beacon.class, null));
//		decoders.put("39428", new BpskAx25Decoder(predict, props, Ax25Beacon.class, null));
//		decoders.put("42790", new Gomx1Decoder(predict, props, CspBeacon.class, false, true, true));
//		decoders.put("49017", new ItSpinsDecoder(predict, props, Ax25Beacon.class));
//		decoders.put("47960", new UspDecoder(predict, props, UspBeacon.class));
//		decoders.put("47952", new UspDecoder(predict, props, UspBeacon.class));
//		decoders.put("47951", new UspDecoder(predict, props, UspBeacon.class));
//		decoders.put("47963", new Diy1Decoder(predict, props));
//		decoders.put("47964", new Smog1Decoder(predict, props));
//		decoders.put("51074", new DelfiPqDecoder(predict, props));
//		decoders.put("51439", new GaspacsDecoder(predict, props));

		for (Satellite cur : satelliteDao.findAll()) {
			for (Transmitter transmitter : cur.getTransmitters()) {
				if (transmitter.getFraming() == null || transmitter.getModulation() == null || transmitter.getBeaconClass() == null) {
					continue;
				}
				if (transmitter.getModulation().equals(Modulation.LORA)) {
					index(cur.getId(), transmitter.getId(), new R2loraDecoder(transmitter.getBeaconClass()));
					continue;
				}
				if (transmitter.getBaudRates() == null || transmitter.getBaudRates().isEmpty()) {
					continue;
				}
				if( transmitter.getFraming().equals(Framing.CUSTOM) ) {
					continue;
				}
				if (transmitter.getModulation().equals(Modulation.GFSK) && transmitter.getFraming().equals(Framing.AX25G3RUH)) {
					index(cur.getId(), transmitter.getId(), new FskAx25G3ruhDecoder(predict, props, transmitter.getBeaconClass(), transmitter.getAssistedHeader()));
				} else if (transmitter.getModulation().equals(Modulation.GFSK) && transmitter.getFraming().equals(Framing.AX100)) {
					if (transmitter.getBeaconSizeBytes() == 0) {
						LOG.error("beacon size bytes are missing for GFSK AX100: {}", cur.getId());
						continue;
					}
					index(cur.getId(), transmitter.getId(), new FskAx100Decoder(predict, props, transmitter.getBeaconSizeBytes(), transmitter.getBeaconClass()));
				} else if (transmitter.getModulation().equals(Modulation.BPSK) && transmitter.getFraming().equals(Framing.AX25G3RUH)) {
					index(cur.getId(), transmitter.getId(), new BpskAx25G3ruhDecoder(predict, props, transmitter.getBeaconClass(), transmitter.getAssistedHeader()));
				} else if (transmitter.getModulation().equals(Modulation.BPSK) && transmitter.getFraming().equals(Framing.AX25)) {
					index(cur.getId(), transmitter.getId(), new BpskAx25Decoder(predict, props, transmitter.getBeaconClass(), transmitter.getAssistedHeader()));
				} else if (transmitter.getModulation().equals(Modulation.AFSK) && transmitter.getFraming().equals(Framing.AX25)) {
					index(cur.getId(), transmitter.getId(), new AfskAx25Decoder(predict, props, transmitter.getBeaconClass(), transmitter.getAssistedHeader()));
				} else {
					LOG.error("unsupported combination of modulation and framing: {} - {}", transmitter.getModulation(), transmitter.getFraming());
				}
			}
		}

		validateDecoders();
	}

	public Decoder findByKey(String satelliteId, String transmitterId) {
		return decoders.get(new DecoderKey(satelliteId, transmitterId));
	}

	private void index(String satelliteId, String transmitterId, Decoder decoder) {
		decoders.put(new DecoderKey(satelliteId, transmitterId), decoder);
	}

	private void validateDecoders() {
		for (Satellite cur : satelliteDao.findAll()) {
			for (Transmitter curTransmitter : cur.getTransmitters()) {
				if (!decoders.containsKey(new DecoderKey(cur.getId(), curTransmitter.getId()))) {
					throw new IllegalStateException("decoder is not defined for satellite: " + cur.getId() + " transmitter: " + curTransmitter.getId());
				}
			}
		}
		for (DecoderKey id : decoders.keySet()) {
			Satellite satellite = satelliteDao.findById(id.getSatelliteId());
			if (satellite == null) {
				throw new IllegalStateException("missing satellite configuration for: " + id);
			}
			Transmitter transmitter = satellite.getById(id.getTransmitterId());
			if (transmitter == null) {
				throw new IllegalStateException("missing transmitter configuration for: " + id);
			}
		}
	}

}
