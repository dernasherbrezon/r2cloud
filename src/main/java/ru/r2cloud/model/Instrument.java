package ru.r2cloud.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import ru.r2cloud.util.SignedURL;

public class Instrument {

	private String id;
	private String name;
	private String satdumpName;
	private String satdumpCombined;
	private File combinedImage;
	private String combinedImageURL;
	private String description;
	private List<InstrumentChannel> channels;
	private String satdumpImageSeries;
	private List<File> imageSeries;
	private List<String> imageSeriesURL;
	private boolean enabled;
	private boolean primary;
	private boolean series;

	public Instrument() {
		// do nothing
	}

	public Instrument(Instrument other) {
		this.id = other.id;
		this.name = other.name;
		this.satdumpName = other.satdumpName;
		this.satdumpCombined = other.satdumpCombined;
		this.combinedImage = other.combinedImage;
		this.combinedImageURL = other.combinedImageURL;
		this.description = other.description;
		if (other.channels != null) {
			this.channels = new ArrayList<>(other.channels.size());
			for (InstrumentChannel cur : other.channels) {
				this.channels.add(new InstrumentChannel(cur));
			}
		}
		this.enabled = other.enabled;
		this.primary = other.primary;
		this.series = other.series;
		this.satdumpImageSeries = other.satdumpImageSeries;
		if (other.imageSeriesURL != null) {
			this.imageSeriesURL = new ArrayList<>(other.imageSeriesURL);
		}
		if (other.imageSeries != null) {
			this.imageSeries = new ArrayList<>(other.imageSeries);
		}
	}

	public String getSatdumpImageSeries() {
		return satdumpImageSeries;
	}

	public void setSatdumpImageSeries(String satdumpImageSeries) {
		this.satdumpImageSeries = satdumpImageSeries;
	}

	public List<File> getImageSeries() {
		return imageSeries;
	}

	public void setImageSeries(List<File> imageSeries) {
		this.imageSeries = imageSeries;
	}

	public List<String> getImageSeriesURL() {
		return imageSeriesURL;
	}

	public void setImageSeriesURL(List<String> imageSeriesURL) {
		this.imageSeriesURL = imageSeriesURL;
	}

	public boolean isSeries() {
		return series;
	}

	public void setSeries(boolean series) {
		this.series = series;
	}

	public File getCombinedImage() {
		return combinedImage;
	}

	public void setCombinedImage(File combinedImage) {
		this.combinedImage = combinedImage;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSatdumpName() {
		return satdumpName;
	}

	public void setSatdumpName(String satdumpName) {
		this.satdumpName = satdumpName;
	}

	public String getSatdumpCombined() {
		return satdumpCombined;
	}

	public void setSatdumpCombined(String satdumpCombined) {
		this.satdumpCombined = satdumpCombined;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<InstrumentChannel> getChannels() {
		return channels;
	}

	public void setChannels(List<InstrumentChannel> channels) {
		this.channels = channels;
	}

	public String getCombinedImageURL() {
		return combinedImageURL;
	}

	public void setCombinedImageURL(String combinedImageURL) {
		this.combinedImageURL = combinedImageURL;
	}

	public static Instrument fromJson(JsonObject obj) {
		Instrument result = new Instrument();
		result.setId(obj.getString("id", null));
		if (result.getId() == null) {
			return null;
		}
		result.setEnabled(obj.getBoolean("enabled", false));
		result.setPrimary(obj.getBoolean("primary", false));
		result.setSeries(obj.getBoolean("series", false));
		result.setName(obj.getString("name", null));
		result.setSatdumpName(obj.getString("satdumpName", null));
		result.setSatdumpCombined(obj.getString("satdumpCombined", null));
		result.setDescription(obj.getString("description", null));
		JsonValue channels = obj.get("channels");
		if (channels != null && channels.isArray()) {
			List<InstrumentChannel> instrumentChannels = new ArrayList<>();
			JsonArray channelsArray = channels.asArray();
			for (int i = 0; i < channelsArray.size(); i++) {
				InstrumentChannel cur = InstrumentChannel.fromJson(channelsArray.get(i).asObject());
				if (cur == null) {
					continue;
				}
				instrumentChannels.add(cur);
			}
			result.setChannels(instrumentChannels);
		}
		result.setSatdumpImageSeries(obj.getString("satdumpImageSeries", null));
		return result;
	}

	public JsonObject toJson(SignedURL signed) {
		JsonObject result = new JsonObject();
		result.add("id", id);
		result.add("enabled", enabled);
		result.add("primary", primary);
		result.add("series", series);
		if (name != null) {
			result.add("name", name);
		}
		if (satdumpName != null) {
			result.add("satdumpName", satdumpName);
		}
		if (satdumpCombined != null) {
			result.add("satdumpCombined", satdumpCombined);
		}
		if (description != null) {
			result.add("description", description);
		}
		if (combinedImageURL != null) {
			if (signed != null) {
				result.add("combinedImageURL", signed.sign(combinedImageURL));
			} else {
				result.add("combinedImageURL", combinedImageURL);
			}
		}
		if (imageSeriesURL != null) {
			JsonArray seriesArray = new JsonArray();
			for (String cur : imageSeriesURL) {
				if (signed != null) {
					seriesArray.add(signed.sign(cur));
				} else {
					seriesArray.add(cur);
				}
			}
			result.add("imageSeriesURL", seriesArray);
		}
		if (satdumpImageSeries != null) {
			result.add("satdumpImageSeries", satdumpImageSeries);
		}
		if (channels != null) {
			JsonArray channelsArrays = new JsonArray();
			for (InstrumentChannel cur : channels) {
				channelsArrays.add(cur.toJson(signed));
			}
			result.add("channels", channelsArrays);
		}

		return result;
	}

}
