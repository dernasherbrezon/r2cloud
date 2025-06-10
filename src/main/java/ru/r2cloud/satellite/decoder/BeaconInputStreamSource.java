package ru.r2cloud.satellite.decoder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.fec.ccsds.UncorrectableException;
import ru.r2cloud.util.Util;

public class BeaconInputStreamSource<T extends Beacon> extends BeaconSource<T> {

	private final BeaconInputStream<T> stream;

	public BeaconInputStreamSource(File bin, Class<T> clazz) throws IOException {
		stream = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(bin)), clazz);
	}

	@Override
	public boolean hasNext() {
		boolean result = stream.hasNext();
		if (!result) {
			// just in case close is not called explicitly
			Util.closeQuietly(stream);
		}
		return result;
	}

	@Override
	public T next() {
		return stream.next();
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}

	@Override
	protected T parseBeacon(byte[] raw) throws UncorrectableException, IOException {
		// do nothing
		return null;
	}
}
