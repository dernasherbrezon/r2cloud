package ru.r2cloud.spyserver;

import java.io.InputStream;

public interface OnDataCallback {

	void onData(InputStream is, int len);

}
