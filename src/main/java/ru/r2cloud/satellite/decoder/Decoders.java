package ru.r2cloud.satellite.decoder;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.ax25.Ax25Beacon;
import ru.r2cloud.jradio.ccsds.TransferFrame;
import ru.r2cloud.jradio.fox.Fox1BBeacon;
import ru.r2cloud.jradio.fox.Fox1CBeacon;
import ru.r2cloud.jradio.fox.Fox1DBeacon;
import ru.r2cloud.jradio.geoscan.Geoscan2Beacon;
import ru.r2cloud.jradio.mrc100.Mrc100Beacon;
import ru.r2cloud.jradio.usp.UspBeacon;
import ru.r2cloud.model.DecoderKey;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class Decoders {

	private static final Logger LOG = LoggerFactory.getLogger(Decoders.class);

	private final Map<DecoderKey, Decoder> decoders = new HashMap<>();
	private final PredictOreKit predict;
	private final Configuration props;
	private final ProcessFactory processFactory;
	private final ThreadPoolFactory threadFactory;

	public Decoders(PredictOreKit predict, Configuration props, ProcessFactory processFactory, ThreadPoolFactory threadFactory) {
		this.predict = predict;
		this.props = props;
		this.processFactory = processFactory;
		this.threadFactory = threadFactory;
		index("32789", "32789-0", new DelfiC3Decoder(predict, props));
		index("39444", "39444-0", new Ao73Decoder(predict, props));
		index("41460", "41460-0", new Aausat4Decoder(predict, props));
		index("42017", "42017-0", new Nayif1Decoder(predict, props));
		index("42784", "42784-0", new PegasusDecoder(predict, props));
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
		index("44406", "44406-0", new Lucky7Decoder(predict, props));
		index("44878", "44878-0", new OpsSatDecoder(predict, props));
		index("44885", "44885-0", new Floripasat1Decoder(predict, props));
		index("43017", "43017-0", new FoxSlowDecoder<>(predict, props, Fox1BBeacon.class));
		index("43770", "43770-0", new FoxSlowDecoder<>(predict, props, Fox1CBeacon.class));
		index("43137", "43137-0", new FoxDecoder<>(predict, props, Fox1DBeacon.class));
		index("43855", "43855-0", new ChompttDecoder(predict, props));
		index("41789", "41789-0", new Alsat1nDecoder(predict, props));
		index("46495", "46495-0", new SalsatDecoder(predict, props));
		index("49017", "49017-0", new ItSpinsDecoder(predict, props, Ax25Beacon.class));
		index("47960", "47960-0", new UspDecoder(predict, props, UspBeacon.class));
		index("47952", "47952-0", new UspDecoder(predict, props, UspBeacon.class));
		index("47951", "47951-0", new UspDecoder(predict, props, UspBeacon.class));
		index("47963", "47963-0", new Diy1Decoder(predict, props));
		index("47964", "47964-0", new Smog1Decoder(predict, props));
		index("51439", "51439-0", new GaspacsDecoder(predict, props));
		index("53108", "53108-0", new CcsdsDecoder(predict, props, TransferFrame.class));
		index("56993", "56993-0", new Mrc100Decoder(predict, props, Mrc100Beacon.class));
		index("56211", "56211-1", new InspireSat7SpinoDecoder(predict, props));
		index("57167", "57167-0", new StratosatTk1Decoder(predict, props));
		index("55104", "55104-0", new Sharjahsat1Decoder(predict, props));
		index("56212", "56212-0", new RoseyDecoder(predict, props));
		index("53384", "53384-0", new SputnixDecoder(predict, props));
		index("61757", "61757-0", new Geoscan2Decoder(predict, props, Geoscan2Beacon.class, 74));
		index("61785", "61785-0", new Geoscan2Decoder(predict, props, Geoscan2Beacon.class, 74));
		index("61746", "61746-0", new Geoscan2Decoder(predict, props, Geoscan2Beacon.class, 74));
		index("61749", "61749-0", new Geoscan2Decoder(predict, props, Geoscan2Beacon.class, 74));
		index("61782", "61782-0", new Geoscan2Decoder(predict, props, Geoscan2Beacon.class, 74));
	}

	public Decoder findByTransmitter(Transmitter transmitter) {
		if (transmitter == null) {
			return null;
		}
		if (transmitter.getFraming().equals(Framing.CUSTOM)) {
			Decoder result = decoders.get(new DecoderKey(transmitter.getSatelliteId(), transmitter.getId()));
			if (result == null) {
				LOG.error("unable to find decoder for custom framing: {}", transmitter.getId());
			}
			return result;
		}
		if (transmitter.getFraming().equals(Framing.LRPT)) {
			return new LRPTDecoder(predict, props);
		}
		if (transmitter.getFraming().equals(Framing.SATDUMP)) {
			return new SatdumpDecoder(props, processFactory);
		}
		if (transmitter.getFraming().equals(Framing.APT)) {
			return new APTDecoder(props, processFactory, threadFactory);
		}
		if (transmitter.getFraming() == null || transmitter.getBeaconClass() == null) {
			LOG.error("framing or modulation or beacon class are empty for: {}", transmitter.getId());
			return null;
		}
		if (transmitter.getFraming().equals(Framing.LORA)) {
			return new LoraDecoder(transmitter.getBeaconClass());
		}
		if (transmitter.getBaudRates() == null || transmitter.getBaudRates().isEmpty()) {
			LOG.error("baud rates are missing: {}", transmitter.getId());
			return null;
		}

		if (transmitter.getFraming().equals(Framing.AX25G3RUH)) {
			return new Ax25G3ruhDecoder(predict, props, transmitter.getBeaconClass(), transmitter.getAssistedHeader());
		} else if (transmitter.getFraming().equals(Framing.AX25)) {
			return new Ax25Decoder(predict, props, transmitter.getBeaconClass(), transmitter.getAssistedHeader());
		} else if (transmitter.getFraming().equals(Framing.AX100)) {
			String syncword = "10010011000010110101000111011110";
			if (transmitter.getSyncword() != null) {
				syncword = transmitter.getSyncword();
			}
			return new Ax100Decoder(predict, props, transmitter.getBeaconClass(), syncword);
		} else if (transmitter.getFraming().equals(Framing.U482C)) {
			String syncword = "11000011101010100110011001010101";
			if (transmitter.getSyncword() != null) {
				syncword = transmitter.getSyncword();
			}
			return new U482cDecoder(predict, props, transmitter.getBeaconClass(), syncword);
		} else if (transmitter.getFraming().equals(Framing.USP)) {
			return new UspDecoder(predict, props, transmitter.getBeaconClass());
		} else if (transmitter.getFraming().equals(Framing.TUBIX20)) {
			return new TUBiX20Decoder(predict, props, transmitter.getBeaconClass());
		} else if (transmitter.getFraming().equals(Framing.MOBITEX)) {
			return new MobitexDecoder(predict, props, transmitter.getBeaconClass());
		} else if (transmitter.getFraming().equals(Framing.OPENLST)) {
			return new OpenLstDecoder(predict, props, transmitter.getBeaconClass());
		} else if (transmitter.getFraming().equals(Framing.CCSDS)) {
			return new CcsdsDecoder(predict, props, transmitter.getBeaconClass());
		} else if (transmitter.getFraming().equals(Framing.CC11XX)) {
			// default
			String syncword = "11010011100100011101001110010001";
			if (transmitter.getSyncword() != null) {
				syncword = transmitter.getSyncword();
			}
			return new Cc11xxDecoder(predict, props, transmitter.getBeaconClass(), syncword, transmitter.getBeaconSizeBytes());
		} else {
			LOG.error("unsupported combination of modulation and framing: {} - {}", transmitter.getModulation(), transmitter.getFraming());
			return null;
		}
	}

	private void index(String satelliteId, String transmitterId, Decoder decoder) {
		decoders.put(new DecoderKey(satelliteId, transmitterId), decoder);
	}

}
