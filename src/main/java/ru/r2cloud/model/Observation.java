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
	private String gain;
	private String channelA;
	private String channelB;
	private Long numberOfDecodedPackets = 0L;

	private String rawURL;
	private File rawPath;

	private String aURL;
	private File imagePath;

	private String spectogramURL;
	private File spectogramPath;

	private String dataURL;
	private File dataPath;

	private ObservationStatus status;
	private boolean biast;
	private SdrType sdrType;
	private long centerBandFrequency;
	
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
		gain = String.valueOf(req.getGain());
		biast = req.isBiast();
		sdrType = req.getSdrType();
		centerBandFrequency = req.getCenterBandFrequency();
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
		if (gain != null) {
			result.setGain(Double.valueOf(gain));
		} else {
			result.setGain(45.0);
		}
		result.setBiast(biast);
		result.setSdrType(sdrType);
		result.setCenterBandFrequency(centerBandFrequency);
		return result;
	}

	public long getCenterBandFrequency() {
		return centerBandFrequency;
	}
	
	public void setCenterBandFrequency(long centerBandFrequency) {
		this.centerBandFrequency = centerBandFrequency;
	}
	
	public SdrType getSdrType() {
		return sdrType;
	}

	public void setSdrType(SdrType sdrType) {
		this.sdrType = sdrType;
	}

	public boolean isBiast() {
		return biast;
	}

	public void setBiast(boolean biast) {
		this.biast = biast;
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

	public String getRawURL() {
		return rawURL;
	}

	public void setRawURL(String rawURL) {
		this.rawURL = rawURL;
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

	public File getImagePath() {
		return imagePath;
	}

	public void setImagePath(File imagePath) {
		this.imagePath = imagePath;
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
		result.setRawURL(meta.getString("rawURL", null));
		String statusStr = meta.getString("status", null);
		if (statusStr != null) {
			result.setStatus(ObservationStatus.valueOf(statusStr));
		} else {
			result.setStatus(ObservationStatus.UPLOADED);
		}
		result.setBiast(meta.getBoolean("biast", false));
		String sdrTypeStr = meta.getString("sdrType", null);
		SdrType sdrType;
		if (sdrTypeStr != null) {
			sdrType = SdrType.valueOf(sdrTypeStr);
		} else {
			sdrType = SdrType.RTLSDR;
		}
		result.setSdrType(sdrType);
		result.setCenterBandFrequency(meta.getLong("centerBandFrequency", 0));
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
		addNullable("aURL", getaURL(), signed, json);
		addNullable("data", getDataURL(), signed, json);
		addNullable("spectogramURL", getSpectogramURL(), signed, json);
		addNullable("rawURL", getRawURL(), signed, json);
		ObservationStatus statusToSave = getStatus();
		if (statusToSave == null) {
			// this would avoid double upload/decode of old observations
			statusToSave = ObservationStatus.UPLOADED;
		}
		json.add("status", statusToSave.name());
		json.add("biast", isBiast());
		json.add("sdrType", sdrType.name());
		json.add("centerBandFrequency", centerBandFrequency);
		return json;
	}

	private static void addNullable(String name, String url, SignedURL signed, JsonObject json) {
		if (url == null) {
			return;
		}
		if (signed != null) {
			json.add(name, signed.sign(url));
		} else {
			json.add(name, url);
		}
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
