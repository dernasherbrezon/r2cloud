package ru.r2cloud.lora.loraat.gatt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BluezServer {

	private static final Logger LOG = LoggerFactory.getLogger(BluezServer.class);

	private final String unixFileSocketPath;

	private EmbeddedDBusDaemon daemon;
	private DBusConnection dbusConn;
	private BluezManager manager = new BluezManager();

	public BluezServer(String unixFileSocketPath) {
		this.unixFileSocketPath = unixFileSocketPath;
	}

	public void start() throws Exception {
		daemon = new EmbeddedDBusDaemon(BusAddress.of("unix:path=" + unixFileSocketPath + ",listen=true"));
		daemon.startInBackgroundAndWait(10000);

		dbusConn = DBusConnectionBuilder.forAddress("unix:path=" + unixFileSocketPath).withShared(false).build();
		dbusConn.requestBusName("org.bluez");
		dbusConn.exportObject(manager);
		dbusConn.exportObject(new BluezApplication(manager));
	}

	public void stop() {
		if (dbusConn != null) {
			try {
				dbusConn.close();
			} catch (IOException e) {
				LOG.error("unable to close dbus connection", e);
			}
		}
		if (daemon != null) {
			try {
				daemon.close();
			} catch (IOException e) {
				LOG.error("can't stop daemon", e);
			}
		}
		try {
			Files.deleteIfExists(Path.of(unixFileSocketPath));
		} catch (IOException e) {
			LOG.error("unable to delete unix socket file: {}", unixFileSocketPath, e);
		}
	}

	public <I extends DBusInterface> I getRemoteObject(String source, String path, Class<I> c) throws DBusException {
		return dbusConn.getRemoteObject(source, path, c);
	}

	public BluezManager getManager() {
		return manager;
	}

}
