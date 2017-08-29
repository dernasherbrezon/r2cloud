package ru.r2cloud.uitl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class Configuration extends Properties {

	private static final Logger LOG = Logger.getLogger(Configuration.class.getName());
	private static final long serialVersionUID = 4175467887601528587L;
	private String propertiesLocation;

	private static Set<PosixFilePermission> MODE600 = new HashSet<PosixFilePermission>();

	static {
		MODE600.add(PosixFilePermission.OWNER_READ);
		MODE600.add(PosixFilePermission.OWNER_WRITE);
	}

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
		try {
			Files.setPosixFilePermissions(Paths.get(propertiesLocation), MODE600);
		} catch (IOException e) {
			LOG.info("unable to setup 600 permissions: " + e.getMessage());
		}
	}

}
