package ru.r2cloud.model;

import java.io.File;
import java.util.Locale;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.util.SignedURL;

public class Observation {

	// observation request
	private String id;
	private long startTimeMillis;
	private long endTimeMillis;
	private String satelliteId;
	private FrequencySource source;
	private long satelliteFrequency;
	private long bandwidth;
	private Tle tle;
	private GeodeticPoint groundStation;

	private int inputSampleRate;
	private int outputSampleRate;
	private long actualFrequency;

	// observation status
	private File rawPath;

	private String gain;
	private String channelA;
	private String channelB;
	private Long numberOfDecodedPackets = 0L;

	private String aURL;
	private File aPath;

	private String spectogramURL;
	private File spectogramPath;

	private String dataURL;
	private File dataPath;

	private ObservationStatus status;

	public Observation() {
		// do nothing
	}

	public Observation(ObservationRequest req) {
		id = req.getId();
		startTimeMillis = req.getStartTimeMillis();
		endTimeMillis = req.getEndTimeMillis();
		satelliteId = req.getSatelliteId();
		source = req.getSource();
		satelliteFrequency = req.getSatelliteFrequency();
		bandwidth = req.getBandwidth();
		tle = req.getTle();
		groundStation = req.getGroundStation();
		inputSampleRate = req.getInputSampleRate();
		outputSampleRate = req.getOutputSampleRate();
		actualFrequency = req.getActualFrequency();
	}

	public ObservationRequest getReq() {
		ObservationRequest result = new ObservationRequest();
		result.setId(id);
		result.setStartTimeMillis(startTimeMillis);
		result.setEndTimeMillis(endTimeMillis);
		result.setSatelliteId(satelliteId);
		result.setSource(source);
		result.setSatelliteFrequency(satelliteFrequency);
		result.setBandwidth(bandwidth);
		result.setTle(tle);
		result.setGroundStation(groundStation);
		result.setInputSampleRate(inputSampleRate);
		result.setOutputSampleRate(outputSampleRate);
		result.setActualFrequency(actualFrequency);
		return result;
	}

	public ObservationStatus getStatus() {
		return status;
	}

	public void setStatus(ObservationStatus status) {
		this.status = status;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getStartTimeMillis() {
		return startTimeMillis;
	}

	public void setStartTimeMillis(long startTimeMillis) {
		this.startTimeMillis = startTimeMillis;
	}

	public long getEndTimeMillis() {
		return endTimeMillis;
	}

	public void setEndTimeMillis(long endTimeMillis) {
		this.endTimeMillis = endTimeMillis;
	}

	public String getSatelliteId() {
		return satelliteId;
	}

	public void setSatelliteId(String satelliteId) {
		this.satelliteId = satelliteId;
	}

	public FrequencySource getSource() {
		return source;
	}

	public void setSource(FrequencySource source) {
		this.source = source;
	}

	public long getSatelliteFrequency() {
		return satelliteFrequency;
	}

	public void setSatelliteFrequency(long satelliteFrequency) {
		this.satelliteFrequency = satelliteFrequency;
	}

	public long getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(long bandwidth) {
		this.bandwidth = bandwidth;
	}

	public Tle getTle() {
		return tle;
	}

	public void setTle(Tle tle) {
		this.tle = tle;
	}

	public GeodeticPoint getGroundStation() {
		return groundStation;
	}

	public void setGroundStation(GeodeticPoint groundStation) {
		this.groundStation = groundStation;
	}

	public int getInputSampleRate() {
		return inputSampleRate;
	}

	public void setInputSampleRate(int inputSampleRate) {
		this.inputSampleRate = inputSampleRate;
	}

	public int getOutputSampleRate() {
		return outputSampleRate;
	}

	public void setOutputSampleRate(int outputSampleRate) {
		this.outputSampleRate = outputSampleRate;
	}

	public long getActualFrequency() {
		return actualFrequency;
	}

	public void setActualFrequency(long actualFrequency) {
		this.actualFrequency = actualFrequency;
	}

	public File getRawPath() {
		return rawPath;
	}

	public void setRawPath(File rawPath) {
		this.rawPath = rawPath;
	}

	public String getGain() {
		return gain;
	}

	public void setGain(String gain) {
		this.gain = gain;
	}

	public String getChannelA() {
		return channelA;
	}

	public void setChannelA(String channelA) {
		this.channelA = channelA;
	}

	public String getChannelB() {
		return channelB;
	}

	public void setChannelB(String channelB) {
		this.channelB = channelB;
	}

	public Long getNumberOfDecodedPackets() {
		return numberOfDecodedPackets;
	}

	public void setNumberOfDecodedPackets(Long numberOfDecodedPackets) {
		this.numberOfDecodedPackets = numberOfDecodedPackets;
	}

	public String getaURL() {
		return aURL;
	}

	public void setaURL(String aURL) {
		this.aURL = aURL;
	}

	public File getaPath() {
		return aPath;
	}

	public void setaPath(File aPath) {
		this.aPath = aPath;
	}

	public String getSpectogramURL() {
		return spectogramURL;
	}

	public void setSpectogramURL(String spectogramURL) {
		this.spectogramURL = spectogramURL;
	}

	public File getSpectogramPath() {
		return spectogramPath;
	}

	public void setSpectogramPath(File spectogramPath) {
		this.spectogramPath = spectogramPath;
	}

	public String getDataURL() {
		return dataURL;
	}

	public void setDataURL(String dataURL) {
		this.dataURL = dataURL;
	}

	public File getDataPath() {
		return dataPath;
	}

	public void setDataPath(File dataPath) {
		this.dataPath = dataPath;
	}

	public static Observation fromJson(JsonObject meta) {
		Observation result = new Observation();
		result.setId(meta.getString("id", null));
		result.setStartTimeMillis(meta.getLong("start", -1L));
		result.setEndTimeMillis(meta.getLong("end", -1L));
		result.setOutputSampleRate(meta.getInt("sampleRate", -1));
		result.setInputSampleRate(meta.getInt("inputSampleRate", -1));
		result.setSatelliteFrequency(meta.getLong("frequency", -1));
		result.setActualFrequency(meta.getLong("actualFrequency", -1));
		result.setBandwidth(meta.getLong("bandwidth", -1));
		String decoder = meta.getString("decoder", null);
		if ("aausat4".equals(decoder)) {
			decoder = "telemetry";
		}
		if (decoder != null) {
			result.setSource(FrequencySource.valueOf(decoder.toUpperCase(Locale.UK)));
		}
		result.setSatelliteId(meta.getString("satellite", null));
		JsonValue tle = meta.get("tle");
		if (tle != null && tle.isObject()) {
			result.setTle(Tle.fromJson(tle.asObject()));
		}
		JsonValue groundStation = meta.get("groundStation");
		if (groundStation != null && groundStation.isObject()) {
			result.setGroundStation(groundStationFromJson(groundStation.asObject()));
		}
		result.setGain(meta.getString("gain", null));
		result.setChannelA(meta.getString("channelA", null));
		result.setChannelB(meta.getString("channelB", null));
		result.setNumberOfDecodedPackets(meta.getLong("numberOfDecodedPackets", 0));
		result.setaURL(meta.getString("aURL", null));
		result.setDataURL(meta.getString("data", null));
		result.setSpectogramURL(meta.getString("spectogramURL", null));
		String statusStr = meta.getString("status", null);
		if (statusStr != null) {
			result.setStatus(ObservationStatus.valueOf(statusStr));
		} else {
			result.setStatus(ObservationStatus.UPLOADED);
		}
		return result;
	}

	public JsonObject toJson(SignedURL signed) {
		JsonObject json = new JsonObject();
		json.add("id", getId());
		json.add("start", getStartTimeMillis());
		json.add("end", getEndTimeMillis());
		json.add("sampleRate", getOutputSampleRate());
		json.add("inputSampleRate", getInputSampleRate());
		json.add("frequency", getSatelliteFrequency());
		json.add("actualFrequency", getActualFrequency());
		json.add("decoder", getSource().name());
		json.add("satellite", getSatelliteId());
		json.add("bandwidth", getBandwidth());
		if (getTle() != null) {
			json.add("tle", getTle().toJson());
		}
		if (getGroundStation() != null) {
			json.add("groundStation", toJson(getGroundStation()));
		}

		if (getGain() != null) {
			json.add("gain", getGain());
		}
		if (getChannelA() != null) {
			json.add("channelA", getChannelA());
		}
		if (getChannelB() != null) {
			json.add("channelB", getChannelB());
		}
		if (getNumberOfDecodedPackets() != null) {
			json.add("numberOfDecodedPackets", getNumberOfDecodedPackets());
		}
		if (getaURL() != null) {
			if (signed != null) {
				json.add("aURL", signed.sign(getaURL()));
			} else {
				json.add("aURL", getaURL());
			}
		}
		if (getDataURL() != null) {
			if (signed != null) {
				json.add("data", signed.sign(getDataURL()));
			} else {
				json.add("data", getDataURL());
			}
		}
		if (getSpectogramURL() != null) {
			if (signed != null) {
				json.add("spectogramURL", signed.sign(getSpectogramURL()));
			} else {
				json.add("spectogramURL", getSpectogramURL());
			}
		}
		ObservationStatus status = getStatus();
		if (status == null) {
			// this would avoid double upload/decode of old observations
			status = ObservationStatus.UPLOADED;
		}
		json.add("status", status.name());
		return json;
	}

	private static GeodeticPoint groundStationFromJson(JsonObject json) {
		double lat = json.getDouble("lat", Double.NaN);
		double lon = json.getDouble("lon", Double.NaN);
		return new GeodeticPoint(FastMath.toRadians(lat), FastMath.toRadians(lon), 0.0);
	}

	private static JsonObject toJson(GeodeticPoint groundStation) {
		JsonObject result = new JsonObject();
		result.add("lat", FastMath.toDegrees(groundStation.getLatitude()));
		result.add("lon", FastMath.toDegrees(groundStation.getLongitude()));
		return result;
	}

	public boolean hasData() {
		return aURL != null || dataURL != null;
	}

}
