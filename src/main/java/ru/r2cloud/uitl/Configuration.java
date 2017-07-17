package ru.r2cloud.uitl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

public class Configuration extends Properties {

	private static final long serialVersionUID = 4175467887601528587L;
	private static final String PROP_NAME = "/config.properties";

	public Configuration() {
		try (InputStream is = Configuration.class.getResourceAsStream(PROP_NAME)) {
			if (is == null) {
				throw new RuntimeException("unable to find properties: " + PROP_NAME);
			}
			load(is);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load properties", e);
		}
	}

	public void update() {
		URL url = Configuration.class.getResource(PROP_NAME);
		File file;
		try {
			file = new File(url.toURI().getPath());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		try (FileWriter fos = new FileWriter(file)) {
			store(fos, "updated");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
