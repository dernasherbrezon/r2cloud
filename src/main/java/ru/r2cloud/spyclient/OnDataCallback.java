package ru.r2cloud.spyclient;

import java.io.InputStream;

public interface OnDataCallback {

	boolean onData(InputStream is, int len);

}
