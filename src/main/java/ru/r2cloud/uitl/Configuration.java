package ru.r2cloud.uitl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration extends Properties {

	private static final long serialVersionUID = 4175467887601528587L;
	private String propertiesLocation;

	public Configuration(String propertiesLocation) {
		try (InputStream is = new FileInputStream(propertiesLocation)) {
			load(is);
		} catch (Exception e) {
			throw new RuntimeException("Unable to load properties", e);
		}
		this.propertiesLocation = System.getProperty("user.home") + File.separator + ".r2cloud";
		if (new File(this.propertiesLocation).exists()) {
			try (InputStream is = new FileInputStream(this.propertiesLocation)) {
				load(is);
			} catch (Exception e) {
				throw new RuntimeException("Unable to load properties", e);
			}
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
