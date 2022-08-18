package ru.r2cloud;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import ru.r2cloud.jradio.Beacon;

public class ClassAdapter extends TypeAdapter<Class<? extends Beacon>> {

	@Override
	public void write(JsonWriter out, Class<? extends Beacon> value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(value.getCanonicalName());
		}
	}

	@Override
	public Class<? extends Beacon> read(JsonReader in) throws IOException {
		if (in != null) {
			try {
				return (Class<? extends Beacon>) Class.forName(in.nextString());
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}

}
