package ru.r2cloud.model;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.jradio.Beacon;

public class Transmitter {

	private String id;
	private Modulation modulation;
	private Framing framing;
	private Class<? extends Beacon> beaconClass;
	// can be used for lora in implicit mode
	private int beaconSizeBytes;
	private long frequency;
	private long frequencyBand;
	private long bandwidth;
	private List<Integer> baudRates;
	private byte[] assistedHeader;
	private boolean bpskDifferential;
	private double bpskCenterFrequency;
	private long deviation;
	private long afCarrier;
	private double transitionWidth;

	private long loraBandwidth;
	private int loraSpreadFactor;
	private int loraSyncword;
	private int loraPreambleLength;
	private int loraLdro;
	private boolean loraExplicitHeader;
	// should be configured only for implicit mode header.
	private int loraCodingRate;
	private boolean loraCrc;
	private TransmitterStatus status;
	private Date updated;

	// copied from satellite
	// to simplify code
	private boolean enabled;
	private String satelliteId;
	private Priority priority;
	private int priorityIndex;
	private Date start;
	private Date end;
	private Tle tle;

	public Transmitter() {
		// do nothing
	}

	public Transmitter(Transmitter copy) {
		this.id = copy.id;
		this.enabled = copy.enabled;
		this.satelliteId = copy.satelliteId;
		this.start = copy.start;
		this.end = copy.end;
		this.priority = copy.priority;
		this.priorityIndex = copy.priorityIndex;
		this.modulation = copy.modulation;
		this.framing = copy.framing;
		this.beaconClass = copy.beaconClass;
		this.beaconSizeBytes = copy.beaconSizeBytes;
		this.frequency = copy.frequency;
		this.frequencyBand = copy.frequencyBand;
		this.bandwidth = copy.bandwidth;
		this.baudRates = copy.baudRates;
		this.assistedHeader = copy.assistedHeader;
		this.bpskDifferential = copy.bpskDifferential;
		this.bpskCenterFrequency = copy.bpskCenterFrequency;
		this.deviation = copy.deviation;
		this.afCarrier = copy.afCarrier;
		this.transitionWidth = copy.transitionWidth;
		this.loraBandwidth = copy.loraBandwidth;
		this.loraSpreadFactor = copy.loraSpreadFactor;
		this.loraCodingRate = copy.loraCodingRate;
		this.loraSyncword = copy.loraSyncword;
		this.loraPreambleLength = copy.loraPreambleLength;
		this.loraLdro = copy.loraLdro;
		this.loraExplicitHeader = copy.loraExplicitHeader;
		this.loraCrc = copy.loraCrc;
		this.tle = copy.tle;
		if (copy.updated != null) {
			this.updated = new Date(copy.updated.getTime());
		}
	}
	
	public int getPriorityIndex() {
		return priorityIndex;
	}
	
	public void setPriorityIndex(int priorityIndex) {
		this.priorityIndex = priorityIndex;
	}
	
	public long getFrequencyBand() {
		return frequencyBand;
	}
	
	public void setFrequencyBand(long frequencyBand) {
		this.frequencyBand = frequencyBand;
	}

	public boolean isLoraCrc() {
		return loraCrc;
	}

	public void setLoraCrc(boolean loraCrc) {
		this.loraCrc = loraCrc;
	}

	public boolean isLoraExplicitHeader() {
		return loraExplicitHeader;
	}

	public void setLoraExplicitHeader(boolean loraExplicitHeader) {
		this.loraExplicitHeader = loraExplicitHeader;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public double getTransitionWidth() {
		return transitionWidth;
	}

	public void setTransitionWidth(double transitionWidth) {
		this.transitionWidth = transitionWidth;
	}

	public long getAfCarrier() {
		return afCarrier;
	}

	public void setAfCarrier(long afCarrier) {
		this.afCarrier = afCarrier;
	}

	public long getDeviation() {
		return deviation;
	}

	public void setDeviation(long deviation) {
		this.deviation = deviation;
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public Tle getTle() {
		return tle;
	}

	public void setTle(Tle tle) {
		this.tle = tle;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Modulation getModulation() {
		return modulation;
	}

	public void setModulation(Modulation modulation) {
		this.modulation = modulation;
	}

	public Framing getFraming() {
		return framing;
	}

	public void setFraming(Framing framing) {
		this.framing = framing;
	}

	public Class<? extends Beacon> getBeaconClass() {
		return beaconClass;
	}

	public void setBeaconClass(Class<? extends Beacon> beaconClass) {
		this.beaconClass = beaconClass;
	}

	public int getBeaconSizeBytes() {
		return beaconSizeBytes;
	}

	public void setBeaconSizeBytes(int beaconSizeBytes) {
		this.beaconSizeBytes = beaconSizeBytes;
	}

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public long getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(long bandwidth) {
		this.bandwidth = bandwidth;
	}

	public List<Integer> getBaudRates() {
		return baudRates;
	}

	public void setBaudRates(List<Integer> baudRates) {
		this.baudRates = baudRates;
	}

	public byte[] getAssistedHeader() {
		return assistedHeader;
	}

	public void setAssistedHeader(byte[] assistedHeader) {
		this.assistedHeader = assistedHeader;
	}

	public boolean isBpskDifferential() {
		return bpskDifferential;
	}

	public void setBpskDifferential(boolean bpskDifferential) {
		this.bpskDifferential = bpskDifferential;
	}

	public double getBpskCenterFrequency() {
		return bpskCenterFrequency;
	}

	public void setBpskCenterFrequency(double bpskCenterFrequency) {
		this.bpskCenterFrequency = bpskCenterFrequency;
	}

	public long getLoraBandwidth() {
		return loraBandwidth;
	}

	public void setLoraBandwidth(long loraBandwidth) {
		this.loraBandwidth = loraBandwidth;
	}

	public int getLoraSpreadFactor() {
		return loraSpreadFactor;
	}

	public void setLoraSpreadFactor(int loraSpreadFactor) {
		this.loraSpreadFactor = loraSpreadFactor;
	}

	public int getLoraCodingRate() {
		return loraCodingRate;
	}

	public void setLoraCodingRate(int loraCodingRate) {
		this.loraCodingRate = loraCodingRate;
	}

	public int getLoraSyncword() {
		return loraSyncword;
	}

	public void setLoraSyncword(int loraSyncword) {
		this.loraSyncword = loraSyncword;
	}

	public int getLoraPreambleLength() {
		return loraPreambleLength;
	}

	public void setLoraPreambleLength(int loraPreambleLength) {
		this.loraPreambleLength = loraPreambleLength;
	}

	public int getLoraLdro() {
		return loraLdro;
	}

	public void setLoraLdro(int loraLdro) {
		this.loraLdro = loraLdro;
	}

	@Override
	public String toString() {
		return id;
	}

	public TransmitterStatus getStatus() {
		return status;
	}

	public void setStatus(TransmitterStatus status) {
		this.status = status;
	}

	@SuppressWarnings("unchecked")
	public static Transmitter fromJson(JsonObject asObject) {
		Transmitter result = new Transmitter();
		result.setFrequency(asObject.getLong("frequency", 0));
		JsonValue bandwidth = asObject.get("bandwidth");
		if (bandwidth != null) {
			result.setBandwidth(bandwidth.asLong());
		}
		JsonValue baudRates = asObject.get("baudRates");
		if (baudRates != null && baudRates.isArray()) {
			JsonArray baudRatesArray = baudRates.asArray();
			List<Integer> bauds = new ArrayList<>(baudRatesArray.size());
			for (int i = 0; i < baudRatesArray.size(); i++) {
				bauds.add(baudRatesArray.get(i).asInt());
			}
			result.setBaudRates(bauds);
		}
		JsonValue modulation = asObject.get("modulation");
		if (modulation != null) {
			try {
				result.setModulation(Modulation.valueOf(modulation.asString()));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		JsonValue framing = asObject.get("framing");
		if (framing != null) {
			try {
				result.setFraming(Framing.valueOf(framing.asString()));
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
		JsonValue beaconClass = asObject.get("beaconClass");
		if (beaconClass != null) {
			try {
				result.setBeaconClass((Class<? extends Beacon>) Class.forName(beaconClass.asString()));
			} catch (ClassNotFoundException e) {
				// server-side might have new beacons, while client side
				// might not yet updated without new beacons
				return null;
			}
		}
		JsonValue beaconSizeBytes = asObject.get("beaconSizeBytes");
		if (beaconSizeBytes != null) {
			result.setBeaconSizeBytes(beaconSizeBytes.asInt());
		}
		JsonValue loraBandwidth = asObject.get("loraBandwidth");
		if (loraBandwidth != null) {
			result.setLoraBandwidth(loraBandwidth.asLong());
		}
		JsonValue loraSpreadFactor = asObject.get("loraSpreadFactor");
		if (loraSpreadFactor != null) {
			result.setLoraSpreadFactor(loraSpreadFactor.asInt());
		}
		JsonValue loraCodingRate = asObject.get("loraCodingRate");
		if (loraCodingRate != null) {
			result.setLoraCodingRate(loraCodingRate.asInt());
		}
		JsonValue loraSyncword = asObject.get("loraSyncword");
		if (loraSyncword != null) {
			result.setLoraSyncword(loraSyncword.asInt());
		}
		JsonValue loraPreambleLength = asObject.get("loraPreambleLength");
		if (loraPreambleLength != null) {
			result.setLoraPreambleLength(loraPreambleLength.asInt());
		}
		JsonValue loraLdro = asObject.get("loraLdro");
		if (loraLdro != null) {
			result.setLoraLdro(loraLdro.asInt());
		}
		JsonValue loraCrc = asObject.get("loraCrc");
		if (loraCrc != null) {
			result.setLoraCrc(loraCrc.asBoolean());
		} else {
			result.setLoraCrc(true);
		}
		JsonValue loraExplicitHeader = asObject.get("loraExplicitHeader");
		if (loraExplicitHeader != null) {
			result.setLoraExplicitHeader(loraExplicitHeader.asBoolean());
		} else {
			result.setLoraExplicitHeader(true);
		}
		JsonValue assistedHeader = asObject.get("assistedHeader");
		if (assistedHeader != null) {
			result.setAssistedHeader(Base64.getDecoder().decode(assistedHeader.asString()));
		}
		JsonValue deviation = asObject.get("deviation");
		if (deviation != null) {
			result.setDeviation(deviation.asLong());
		} else {
			result.setDeviation(5000);
		}
		JsonValue bpskCenterFrequency = asObject.get("bpskCenterFrequency");
		if (bpskCenterFrequency != null) {
			result.setBpskCenterFrequency(bpskCenterFrequency.asDouble());
		}
		JsonValue bpskDifferential = asObject.get("bpskDifferential");
		if (bpskDifferential != null) {
			result.setBpskDifferential(bpskDifferential.asBoolean());
		}
		JsonValue afCarrier = asObject.get("afCarrier");
		if (afCarrier != null) {
			result.setAfCarrier(afCarrier.asLong());
		}
		JsonValue transitionWidth = asObject.get("transitionWidth");
		if (transitionWidth != null) {
			result.setTransitionWidth(transitionWidth.asDouble());
		} else {
			result.setTransitionWidth(2000);
		}
		JsonValue status = asObject.get("status");
		if (status != null) {
			try {
				result.setStatus(TransmitterStatus.valueOf(status.asString()));
			} catch (IllegalArgumentException e) {
				result.setStatus(TransmitterStatus.ENABLED);
			}
		} else {
			result.setStatus(TransmitterStatus.ENABLED);
		}
		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		if (modulation != null) {
			result.add("modulation", modulation.name());
		}
		if (framing != null) {
			result.add("framing", framing.name());
		}
		if (beaconClass != null) {
			result.add("beaconClass", beaconClass.getCanonicalName());
		}
		if (beaconSizeBytes != 0) {
			result.add("beaconSizeBytes", beaconSizeBytes);
		}
		result.add("frequency", frequency);
		if (bandwidth != 0) {
			result.add("bandwidth", bandwidth);
		}
		if (baudRates != null) {
			JsonArray baudRatesArray = new JsonArray();
			for (Integer cur : baudRates) {
				baudRatesArray.add(cur);
			}
			result.add("baudRates", baudRatesArray);
		}
		if (assistedHeader != null) {
			result.add("assistedHeader", Base64.getEncoder().encodeToString(assistedHeader));
		}
		if (status != null) {
			result.add("status", status.name());
		}
		if (loraBandwidth != 0) {
			result.add("loraBandwidth", loraBandwidth);
		}
		if (loraSpreadFactor != 0) {
			result.add("loraSpreadFactor", loraSpreadFactor);
		}
		if (loraCodingRate != 0) {
			result.add("loraCodingRate", loraCodingRate);
		}
		if (loraSyncword != 0) {
			result.add("loraSyncword", loraSyncword);
		}
		if (loraPreambleLength != 0) {
			result.add("loraPreambleLength", loraPreambleLength);
		}
		if (loraLdro != 0) {
			result.add("loraLdro", loraLdro);
		}
		result.add("loraExplicitHeader", loraExplicitHeader);
		result.add("loraCrc", loraCrc);
		if (deviation != 5000) {
			result.add("deviation", deviation);
		}
		if (bpskCenterFrequency != 0) {
			result.add("bpskCenterFrequency", bpskCenterFrequency);
		}
		if (bpskDifferential) {
			result.add("bpskDifferential", bpskDifferential);
		}
		if (afCarrier != 0) {
			result.add("afCarrier", afCarrier);
		}
		if (transitionWidth != 2000) {
			result.add("transitionWidth", transitionWidth);
		}
		return result;
	}

}
