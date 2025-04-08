package ru.r2cloud.model;

import java.io.File;

import com.eclipsesource.json.JsonObject;

import ru.r2cloud.util.SignedURL;

public class InstrumentChannel {

	private String id;
	private String satdumpName;
	private String description;
	private File imagePath;
	private String imageURL;

	public InstrumentChannel() {
		// do nothing
	}

	public InstrumentChannel(InstrumentChannel other) {
		this.id = other.id;
		this.satdumpName = other.satdumpName;
		this.description = other.description;
		this.imagePath = other.imagePath;
		this.imageURL = other.imageURL;
	}

	public String getSatdumpName() {
		return satdumpName;
	}

	public void setSatdumpName(String satdumpName) {
		this.satdumpName = satdumpName;
	}

	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}

	public File getImagePath() {
		return imagePath;
	}

	public void setImagePath(File imagePath) {
		this.imagePath = imagePath;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public static InstrumentChannel fromJson(JsonObject obj) {
		InstrumentChannel result = new InstrumentChannel();
		result.setId(obj.getString("id", null));
		if (result.getId() == null) {
			return null;
		}
		result.setDescription(obj.getString("description", null));
		result.setSatdumpName(obj.getString("satdumpName", null));
		return result;
	}

	public JsonObject toJson(SignedURL signed) {
		JsonObject result = new JsonObject();
		result.add("id", id);
		if (description != null) {
			result.add("description", description);
		}
		if (imageURL != null) {
			if (signed != null) {
				result.add("imageURL", signed.sign(imageURL));
			} else {
				result.add("imageURL", imageURL);
			}
		}
		if (satdumpName != null) {
			result.add("satdumpName", satdumpName);
		}
		return result;
	}

}
