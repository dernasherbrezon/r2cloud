package ru.r2cloud.uitl;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration extends Properties {

	private static final long serialVersionUID = 4175467887601528587L;
	private String propertiesLocation;

	public Configuration(String propertiesLocation) {
		this.propertiesLocation = propertiesLocation;
		try (InputStream is = new FileInputStream(propertiesLocation)) {
			load(is);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load properties", e);
		}
	}

	public void update() {
		try (FileWriter fos = new FileWriter(propertiesLocation)) {
			store(fos, "updated");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
