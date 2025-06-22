package ru.r2cloud.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.util.SignedURL;

public class Observation {

	// observation request
	private String id;
	private long startTimeMillis;
	private long endTimeMillis;
	private String satelliteId;
	private String transmitterId;
	private Tle tle;
	private GeodeticPoint groundStation;

	private DataFormat dataFormat;
	private long sampleRate;
	private long frequency;
	private DeviceConfiguration device;

	// observation status
	private Long numberOfDecodedPackets;
	private Long totalSize = 0L;

	private String rawURL;
	private File rawPath;
	private String sigmfDataURL;
	private String sigmfMetaURL;

	private String channelA;
	private String channelB;
	private String aURL;
	private File imagePath;
	private List<Instrument> instruments;

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
		transmitterId = req.getTransmitterId();
		tle = req.getTle();
		groundStation = req.getGroundStation();
		frequency = req.getFrequency();
	}

	public ObservationRequest getReq() {
		ObservationRequest result = new ObservationRequest();
		result.setId(id);
		result.setStartTimeMillis(startTimeMillis);
		result.setEndTimeMillis(endTimeMillis);
		result.setSatelliteId(satelliteId);
		result.setTransmitterId(transmitterId);
		result.setTle(tle);
		result.setGroundStation(groundStation);
		result.setFrequency(frequency);
		return result;
	}

	public DeviceConfiguration getDevice() {
		return device;
	}

	public void setDevice(DeviceConfiguration device) {
		this.device = device;
	}

	public String getSigmfDataURL() {
		return sigmfDataURL;
	}

	public void setSigmfDataURL(String sigmfDataURL) {
		this.sigmfDataURL = sigmfDataURL;
	}

	public String getSigmfMetaURL() {
		return sigmfMetaURL;
	}

	public void setSigmfMetaURL(String sigmfMetaURL) {
		this.sigmfMetaURL = sigmfMetaURL;
	}

	public DataFormat getDataFormat() {
		return dataFormat;
	}

	public void setDataFormat(DataFormat dataFormat) {
		this.dataFormat = dataFormat;
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

	public long getFrequency() {
		return frequency;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
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

	public Long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(Long totalSize) {
		this.totalSize = totalSize;
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

	public String getTransmitterId() {
		return transmitterId;
	}

	public void setTransmitterId(String transmitterId) {
		this.transmitterId = transmitterId;
	}

	public long getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(long sampleRate) {
		this.sampleRate = sampleRate;
	}

	public List<Instrument> getInstruments() {
		return instruments;
	}

	public void setInstruments(List<Instrument> instruments) {
		this.instruments = instruments;
	}

	public static Observation fromJson(JsonObject meta) {
		Observation result = new Observation();
		result.setId(meta.getString("id", null));
		result.setStartTimeMillis(meta.getLong("start", -1L));
		result.setEndTimeMillis(meta.getLong("end", -1L));
		result.setSatelliteId(meta.getString("satellite", null));
		result.setTransmitterId(meta.getString("transmitterId", result.getSatelliteId() + "-0"));
		JsonValue tle = meta.get("tle");
		if (tle != null && tle.isObject()) {
			result.setTle(Tle.fromJson(tle.asObject()));
		}
		JsonValue groundStation = meta.get("groundStation");
		if (groundStation != null && groundStation.isObject()) {
			result.setGroundStation(groundStationFromJson(groundStation.asObject()));
		}
		String dataFormatStr = meta.getString("dataFormat", null);
		if (dataFormatStr != null) {
			result.setDataFormat(DataFormat.valueOf(dataFormatStr));
		}
		int legacyInputRate = meta.getInt("inputSampleRate", 0);
		if (legacyInputRate != 0) {
			result.setSampleRate(legacyInputRate);
		} else {
			result.setSampleRate(meta.getLong("sampleRate", -1));
		}
		result.setFrequency(meta.getLong("actualFrequency", -1));
		result.setChannelA(meta.getString("channelA", null));
		result.setChannelB(meta.getString("channelB", null));
		JsonValue numberOfDecodedPackets = meta.get("numberOfDecodedPackets");
		if (numberOfDecodedPackets != null) {
			result.setNumberOfDecodedPackets(numberOfDecodedPackets.asLong());
		}
		JsonValue totalSize = meta.get("totalSize");
		if (totalSize != null) {
			result.setTotalSize(totalSize.asLong());
		}
		result.setRawURL(meta.getString("rawURL", null));
		result.setaURL(meta.getString("aURL", null));
		result.setSpectogramURL(meta.getString("spectogramURL", null));
		result.setDataURL(meta.getString("data", null));
		String statusStr = meta.getString("status", null);
		if (statusStr != null) {
			result.setStatus(ObservationStatus.valueOf(statusStr));
		} else {
			result.setStatus(ObservationStatus.UPLOADED);
		}
		JsonValue deviceConfig = meta.get("device");
		if (deviceConfig != null) {
			result.setDevice(DeviceConfiguration.fromJson(deviceConfig.asObject()));
		}
		JsonValue instruments = meta.get("instruments");
		if (instruments != null) {
			JsonArray instrumentsArray = instruments.asArray();
			List<Instrument> instrumentsObj = new ArrayList<>();
			for (int i = 0; i < instrumentsArray.size(); i++) {
				Instrument cur = Instrument.fromJson(instrumentsArray.get(i).asObject());
				if (cur == null) {
					continue;
				}
				instrumentsObj.add(cur);
			}
			if (!instrumentsObj.isEmpty()) {
				result.setInstruments(instrumentsObj);
			}
		}
		return result;
	}

	public JsonObject toJson(SignedURL signed) {
		JsonObject json = new JsonObject();
		json.add("id", getId());
		json.add("start", getStartTimeMillis());
		json.add("end", getEndTimeMillis());
		json.add("satellite", getSatelliteId());
		json.add("transmitterId", getTransmitterId());
		if (getTle() != null) {
			json.add("tle", getTle().toJson());
		}
		if (getGroundStation() != null) {
			json.add("groundStation", toJson(getGroundStation()));
		}
		if (dataFormat != null) {
			json.add("dataFormat", dataFormat.name());
		}
		json.add("sampleRate", getSampleRate());
		json.add("actualFrequency", getFrequency());

		if (getChannelA() != null) {
			json.add("channelA", getChannelA());
		}
		if (getChannelB() != null) {
			json.add("channelB", getChannelB());
		}
		if (getNumberOfDecodedPackets() != null) {
			json.add("numberOfDecodedPackets", getNumberOfDecodedPackets());
		}
		if (totalSize != null) {
			json.add("totalSize", getTotalSize());
		}
		addNullable("rawURL", getRawURL(), signed, json);
		addNullable("aURL", getaURL(), signed, json);
		addNullable("spectogramURL", getSpectogramURL(), signed, json);
		addNullable("data", getDataURL(), signed, json);
		addNullable("sigmfDataURL", getSigmfDataURL(), signed, json);
		addNullable("sigmfMetaURL", getSigmfMetaURL(), signed, json);
		ObservationStatus statusToSave = getStatus();
		if (statusToSave == null) {
			// this would avoid double upload/decode of old observations
			statusToSave = ObservationStatus.UPLOADED;
		}
		json.add("status", statusToSave.name());
		if (device != null) {
			json.add("device", device.toJson());
		}
		if (getInstruments() != null && !getInstruments().isEmpty()) {
			JsonArray instrumentsArray = new JsonArray();
			for (Instrument cur : getInstruments()) {
				instrumentsArray.add(cur.toJson(signed));
			}
			json.add("instruments", instrumentsArray);
		}
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
		return aURL != null || dataURL != null || instruments != null;
	}

}
