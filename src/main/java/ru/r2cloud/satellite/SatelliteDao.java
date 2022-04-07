package ru.r2cloud.satellite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.cloud.R2ServerClient;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.model.BandFrequency;
import ru.r2cloud.model.Framing;
import ru.r2cloud.model.Modulation;
import ru.r2cloud.model.Priority;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.SatelliteComparator;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.TransmitterComparator;
import ru.r2cloud.util.Configuration;

public class SatelliteDao {

	private static final Logger LOG = LoggerFactory.getLogger(SatelliteDao.class);

	private final Configuration config;
	private final R2ServerClient r2server;
	private final List<Satellite> satellites = new ArrayList<>();
	private final Map<String, Satellite> satelliteByName = new HashMap<>();
	private final Map<String, Satellite> satelliteById = new HashMap<>();

	public SatelliteDao(Configuration config, R2ServerClient r2server) {
		this.config = config;
		this.r2server = r2server;
		reload();
	}

	public synchronized void reload() {
		satellites.clear();
		satelliteByName.clear();
		satelliteById.clear();

		satellites.addAll(loadFromConfig(config));
		if (config.getBoolean("r2cloud.newLaunches")) {
			satellites.addAll(r2server.loadNewLaunches());
		}
		List<Transmitter> allTransmitters = new ArrayList<>();
		for (Satellite curSatellite : satellites) {
			allTransmitters.addAll(curSatellite.getTransmitters());
			for (Transmitter curTransmitter : curSatellite.getTransmitters()) {
				switch (curTransmitter.getFraming()) {
				case APT:
					curTransmitter.setInputSampleRate(60_000);
					curTransmitter.setOutputSampleRate(11_025);
					break;
				case LRPT:
					curTransmitter.setInputSampleRate(288_000);
					curTransmitter.setOutputSampleRate(144_000);
					break;
				default:
					// sdr-server supports very narrow bandwidths
					int outputSampleRate = 48_000;
					if (config.getSdrType().equals(SdrType.SDRSERVER)) {
						curTransmitter.setInputSampleRate(outputSampleRate);
						curTransmitter.setOutputSampleRate(outputSampleRate);
					} else if (curTransmitter.getModulation() != null && curTransmitter.getModulation().equals(Modulation.LORA)) {
						// not applicable
						curTransmitter.setInputSampleRate(0);
						curTransmitter.setOutputSampleRate(0);
					} else {
						// some rates better to sample at 50k
						if (curTransmitter.getBaudRates() != null && curTransmitter.getBaudRates().size() > 0 && 50_000 % curTransmitter.getBaudRates().get(0) == 0) {
							outputSampleRate = 50_000;
						}
						// 48k * 5 = 240k - minimum rate rtl-sdr supports
						curTransmitter.setInputSampleRate(outputSampleRate * 5);
						curTransmitter.setOutputSampleRate(outputSampleRate);
					}
					break;
				}
			}
			index(curSatellite);
		}
		long sdrServerBandwidth = config.getLong("satellites.sdrserver.bandwidth");
		long bandwidthCrop = config.getLong("satellites.sdrserver.bandwidth.crop");
		Collections.sort(allTransmitters, TransmitterComparator.INSTANCE);

		// bands can be calculated only when all supported transmitters known
		BandFrequency currentBand = null;
		for (Transmitter cur : allTransmitters) {
			long lowerSatelliteFrequency = cur.getFrequency() - cur.getInputSampleRate() / 2;
			long upperSatelliteFrequency = cur.getFrequency() + cur.getInputSampleRate() / 2;
			// first transmitter or upper frequency out of band
			if (currentBand == null || (currentBand.getUpper() - bandwidthCrop) < upperSatelliteFrequency) {
				currentBand = new BandFrequency();
				currentBand.setLower(lowerSatelliteFrequency - bandwidthCrop);
				currentBand.setUpper(currentBand.getLower() + sdrServerBandwidth);
				currentBand.setCenter(currentBand.getLower() + (currentBand.getUpper() - currentBand.getLower()) / 2);
			}
			cur.setFrequencyBand(currentBand);
		}
		Collections.sort(satellites, SatelliteComparator.ID_COMPARATOR);
	}

	@SuppressWarnings("unchecked")
	private static List<Satellite> loadFromConfig(Configuration config) {
		List<Satellite> result = new ArrayList<>();
		for (String cur : config.getProperties("satellites.supported")) {
			Satellite curSatellite = new Satellite();
			curSatellite.setId(cur);
			String name = config.getProperty("satellites." + curSatellite.getId() + ".name");
			if (name == null) {
				throw new IllegalStateException("unable to find satellite name for: " + cur);
			}
			curSatellite.setName(name);
			curSatellite.setPriority(Priority.NORMAL);
			curSatellite.setEnabled(config.getBoolean("satellites." + curSatellite.getId() + ".enabled"));
			List<Transmitter> transmitters = new ArrayList<>();
			for (String curId : config.getProperties("satellites." + curSatellite.getId() + ".transmitters")) {
				String prefix = "satellites." + curSatellite.getId() + ".transmitter." + curId;
				Transmitter curTransmitter = new Transmitter();
				curTransmitter.setId(curSatellite.getId() + "-" + curId);
				curTransmitter.setEnabled(curSatellite.isEnabled());
				curTransmitter.setPriority(curSatellite.getPriority());
				curTransmitter.setSatelliteId(curSatellite.getId());
				curTransmitter.setStart(curSatellite.getStart());
				curTransmitter.setEnd(curSatellite.getEnd());
				curTransmitter.setFrequency(config.getLong(prefix + ".freq"));
				Long bandwidth = config.getLong(prefix + ".bandwidth");
				if (bandwidth != null) {
					curTransmitter.setBandwidth(bandwidth);
				}
				curTransmitter.setBaudRates(config.getIntegerList(prefix + ".baud"));
				String modulationStr = config.getProperty(prefix + ".modulation");
				if (modulationStr != null) {
					curTransmitter.setModulation(Modulation.valueOf(modulationStr));
				}
				curTransmitter.setFraming(Framing.valueOf(config.getProperty(prefix + ".framing")));
				String beaconClassStr = config.getProperty(prefix + ".beacon");
				if (beaconClassStr != null) {
					try {
						curTransmitter.setBeaconClass((Class<? extends Beacon>) Class.forName(beaconClassStr));
					} catch (ClassNotFoundException e) {
						throw new IllegalArgumentException(e);
					}
				}
				String beaconSizeStr = config.getProperty(prefix + ".beaconSize");
				if (beaconSizeStr != null) {
					curTransmitter.setBeaconSizeBytes(Integer.valueOf(beaconSizeStr));
				}

				Long loraBandwidth = config.getLong(prefix + ".loraBw");
				if (loraBandwidth != null) {
					curTransmitter.setLoraBandwidth(loraBandwidth);
				}
				Integer loraSpreadFactor = config.getInteger(prefix + ".loraSf");
				if (loraSpreadFactor != null) {
					curTransmitter.setLoraSpreadFactor(loraSpreadFactor);
				}
				Integer loraCodingRate = config.getInteger(prefix + ".loraCr");
				if (loraCodingRate != null) {
					curTransmitter.setLoraCodingRate(loraCodingRate);
				}
				Integer loraSyncword = config.getInteger(prefix + ".loraSyncword");
				if (loraSyncword != null) {
					curTransmitter.setLoraSyncword(loraSyncword);
				}
				Integer loraPreambleLength = config.getInteger(prefix + ".loraPreambleLength");
				if (loraPreambleLength != null) {
					curTransmitter.setLoraPreambleLength(loraPreambleLength);
				}
				Integer loraLdro = config.getInteger(prefix + ".loraLdro");
				if (loraLdro != null) {
					curTransmitter.setLoraLdro(loraLdro);
				}
				curTransmitter.setAssistedHeader(config.getByteArray(prefix + ".header"));
				Long deviation = config.getLong(prefix + ".deviation");
				if (deviation != null) {
					curTransmitter.setDeviation(deviation);
				} else {
					curTransmitter.setDeviation(5000);
				}
				Long bpskCenterFrequency = config.getLong(prefix + ".bpskCenterFrequency");
				if (bpskCenterFrequency != null) {
					curTransmitter.setBpskCenterFrequency(bpskCenterFrequency);
				}
				curTransmitter.setBpskDifferential(config.getBoolean(prefix + ".bpskDifferential"));
				Long afCarrier = config.getLong(prefix + ".afCarrier");
				if (afCarrier != null) {
					curTransmitter.setAfCarrier(afCarrier);
				}
				Double transitionWidth = config.getDouble(prefix + ".transitionWidth");
				if (transitionWidth != null) {
					curTransmitter.setTransitionWidth(transitionWidth);
				} else {
					curTransmitter.setTransitionWidth(2000);
				}
				transmitters.add(curTransmitter);
			}
			if (transmitters.isEmpty()) {
				LOG.error("no transmitters found for: {}", name);
				continue;
			}
			curSatellite.setTransmitters(transmitters);

			result.add(curSatellite);
		}
		return result;
	}

	public synchronized Satellite findByName(String name) {
		return satelliteByName.get(name);
	}

	public synchronized Satellite findById(String id) {
		return satelliteById.get(id);
	}

	public synchronized List<Satellite> findAll() {
		return satellites;
	}

	private void index(Satellite satellite) {
		satelliteByName.put(satellite.getName(), satellite);
		satelliteById.put(satellite.getId(), satellite);
	}

	public void update(Satellite satelliteToEdit) {
		config.setProperty("satellites." + satelliteToEdit.getId() + ".enabled", satelliteToEdit.isEnabled());
		config.update();
	}

	public synchronized List<Satellite> findEnabled() {
		List<Satellite> result = new ArrayList<>();
		for (Satellite cur : satellites) {
			if (cur.isEnabled()) {
				result.add(cur);
			}
		}
		return result;
	}

}
