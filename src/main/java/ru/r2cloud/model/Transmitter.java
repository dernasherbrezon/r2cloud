package ru.r2cloud.model;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.ccsds.CcsdsFraming;
import ru.r2cloud.jradio.ccsds.Coding;
import ru.r2cloud.jradio.ccsds.ScramblerType;

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

	private String syncword;

	private LoraModulation loraModulation;
	private PcmpskpmModulation pcmpskpmModulation;
	private GfskModulation gfskModulation;
	private AfskModulation afskModulation;

	private TransmitterStatus status;
	private Date updated;
	private String satdumpPipeline;
	private int satdumpCaduSizeBytes;

	private CcsdsFraming ccsdsFraming;

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
		if (copy.loraModulation != null) {
			this.loraModulation = new LoraModulation(copy.loraModulation);
		}
		if (copy.pcmpskpmModulation != null) {
			this.pcmpskpmModulation = new PcmpskpmModulation(copy.pcmpskpmModulation);
		}
		if (copy.gfskModulation != null) {
			this.gfskModulation = new GfskModulation(copy.gfskModulation);
		}
		if (copy.afskModulation != null) {
			this.afskModulation = new AfskModulation(copy.afskModulation);
		}
		this.tle = copy.tle;
		this.syncword = copy.syncword;
		if (copy.updated != null) {
			this.updated = new Date(copy.updated.getTime());
		}
		this.satdumpPipeline = copy.satdumpPipeline;
		this.satdumpCaduSizeBytes = copy.satdumpCaduSizeBytes;
		if (copy.ccsdsFraming != null) {
			this.ccsdsFraming = new CcsdsFraming(copy.ccsdsFraming);
		}
	}

	public int getSatdumpCaduSizeBytes() {
		return satdumpCaduSizeBytes;
	}

	public void setSatdumpCaduSizeBytes(int satdumpCaduSizeBytes) {
		this.satdumpCaduSizeBytes = satdumpCaduSizeBytes;
	}

	public String getSatdumpPipeline() {
		return satdumpPipeline;
	}

	public void setSatdumpPipeline(String satdumpPipeline) {
		this.satdumpPipeline = satdumpPipeline;
	}

	public String getSyncword() {
		return syncword;
	}

	public void setSyncword(String syncword) {
		this.syncword = syncword;
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

	public LoraModulation getLoraModulation() {
		return loraModulation;
	}

	public void setLoraModulation(LoraModulation loraModulation) {
		this.loraModulation = loraModulation;
	}

	public CcsdsFraming getCcsdsFraming() {
		return ccsdsFraming;
	}

	public void setCcsdsFraming(CcsdsFraming ccsdsFraming) {
		this.ccsdsFraming = ccsdsFraming;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public AfskModulation getAfskModulation() {
		return afskModulation;
	}

	public void setAfskModulation(AfskModulation afskModulation) {
		this.afskModulation = afskModulation;
	}

	public GfskModulation getGfskModulation() {
		return gfskModulation;
	}

	public void setGfskModulation(GfskModulation gfskModulation) {
		this.gfskModulation = gfskModulation;
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

	public PcmpskpmModulation getPcmpskpmModulation() {
		return pcmpskpmModulation;
	}

	public void setPcmpskpmModulation(PcmpskpmModulation pcmpskpmModulation) {
		this.pcmpskpmModulation = pcmpskpmModulation;
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
		} else {
			result.setBaudRates(Collections.emptyList());
		}
		JsonValue modulation = asObject.get("modulation");
		if (modulation != null) {
			try {
				result.setModulation(Modulation.valueOf(modulation.asString()));
			} catch (IllegalArgumentException e) {
				return null;
			}
		} else {
			return null;
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
		result.setSyncword(asObject.getString("syncword", null));
		JsonValue beaconSizeBytes = asObject.get("beaconSizeBytes");
		if (beaconSizeBytes != null) {
			result.setBeaconSizeBytes(beaconSizeBytes.asInt());
		}
		if (result.getModulation().equals(Modulation.LORA)) {
			result.setLoraModulation(LoraModulation.fromJson(asObject));
		}
		JsonValue assistedHeader = asObject.get("assistedHeader");
		if (assistedHeader != null) {
			result.setAssistedHeader(Base64.getDecoder().decode(assistedHeader.asString()));
		}
		JsonValue bpskCenterFrequency = asObject.get("bpskCenterFrequency");
		if (bpskCenterFrequency != null) {
			result.setBpskCenterFrequency(bpskCenterFrequency.asDouble());
		}
		JsonValue bpskDifferential = asObject.get("bpskDifferential");
		if (bpskDifferential != null) {
			result.setBpskDifferential(bpskDifferential.asBoolean());
		}
		JsonValue afskModulation = asObject.get("afskModulation");
		if (afskModulation != null) {
			result.setAfskModulation(AfskModulation.fromJson(afskModulation.asObject()));
		} else {
			// backward compatibility
			result.setAfskModulation(AfskModulation.fromJson(asObject));
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
		JsonValue satdumpPipeline = asObject.get("satdumpPipeline");
		if (satdumpPipeline != null) {
			result.setSatdumpPipeline(satdumpPipeline.asString());
		}
		JsonValue satdumpCaduSizeBytes = asObject.get("satdumpCaduSizeBytes");
		if (satdumpCaduSizeBytes != null) {
			result.setSatdumpCaduSizeBytes(satdumpCaduSizeBytes.asInt());
		}
		JsonValue ccsdsFraming = asObject.get("ccsdsFraming");
		if (ccsdsFraming != null) {
			result.setCcsdsFraming(ccsdsFramingFromJson(ccsdsFraming.asObject()));
		}
		JsonValue pcmpskpmModulation = asObject.get("pcmpskpmModulation");
		if (pcmpskpmModulation != null) {
			result.setPcmpskpmModulation(PcmpskpmModulation.fromJson(pcmpskpmModulation.asObject()));
		}
		JsonValue gfskModulation = asObject.get("gfskModulation");
		if (gfskModulation != null) {
			result.setGfskModulation(GfskModulation.fromJson(gfskModulation.asObject()));
		} else {
			// backward compatible
			result.setGfskModulation(GfskModulation.fromJson(asObject));
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
		if (syncword != null) {
			result.add("syncword", syncword);
		}
		if (loraModulation != null) {
			loraModulation.toJson(result);
		}
		if (bpskCenterFrequency != 0) {
			result.add("bpskCenterFrequency", bpskCenterFrequency);
		}
		if (bpskDifferential) {
			result.add("bpskDifferential", bpskDifferential);
		}
		if (afskModulation != null) {
			result.add("afskModulation", afskModulation.toJson());
		}
		if (satdumpPipeline != null) {
			result.add("satdumpPipeline", satdumpPipeline);
		}
		if (satdumpCaduSizeBytes != 0) {
			result.add("satdumpCaduSizeBytes", satdumpCaduSizeBytes);
		}
		if (ccsdsFraming != null) {
			result.add("ccsdsFraming", ccsdsFramingToJson(ccsdsFraming));
		}
		if (pcmpskpmModulation != null) {
			result.add("pcmpskpmModulation", pcmpskpmModulation.toJson());
		}
		if (gfskModulation != null) {
			result.add("gfskModulation", gfskModulation.toJson());
		}
		return result;
	}

	private static JsonObject ccsdsFramingToJson(CcsdsFraming framing) {
		JsonObject result = new JsonObject();
		result.set("coding", framing.getCoding().name());
		result.set("frameLengthBytes", framing.getFrameLength());
		result.set("scrambler", framing.getScrambler().name());
		result.set("syncwordThreshold", framing.getSyncwordThreshold());
		result.set("syncword", framing.getSyncword());
		return result;
	}

	private static CcsdsFraming ccsdsFramingFromJson(JsonObject obj) {
		CcsdsFraming result = new CcsdsFraming();
		result.setCoding(Coding.valueOf(obj.getString("coding", null)));
		result.setFrameLength(obj.getInt("frameLengthBytes", 0));
		result.setScrambler(ScramblerType.valueOf(obj.getString("scrambler", null)));
		result.setSyncwordThreshold(obj.getInt("syncwordThreshold", 0));
		result.setSyncword(obj.getString("syncword", null));
		return result;
	}

}
