package ru.r2cloud;

import java.io.IOException;
import java.util.Date;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class DateAdapter extends TypeAdapter<Date> {

	@Override
	public void write(JsonWriter out, Date value) throws IOException {
		if (value == null) {
			out.nullValue();
		} else {
			out.value(value.getTime());
		}
	}

	@Override
	public Date read(JsonReader in) throws IOException {
		if (in != null) {
			return new Date(in.nextLong());
		} else {
			return null;
		}
	}

}
