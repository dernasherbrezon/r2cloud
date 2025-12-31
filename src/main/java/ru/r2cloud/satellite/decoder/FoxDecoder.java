package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.fox.Fox1DBeacon;
import ru.r2cloud.jradio.fox.FoxPictureDecoder;
import ru.r2cloud.jradio.fox.HighSpeedFox;
import ru.r2cloud.jradio.fox.PictureScanLine;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.Instrument;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class FoxDecoder<T extends Beacon> extends FoxSlowDecoder<T> {

	private final Class<T> clazz;

	public FoxDecoder(PredictOreKit predict, Configuration config, Class<T> clazz) {
		super(predict, config, clazz);
		this.clazz = clazz;
	}

	@SuppressWarnings("resource")
	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, Observation req, final Transmitter transmitter, Integer baudRate) throws IOException {
		DemodulatorType type = config.getDemodulatorType(transmitter.getModulation());
		if (type.equals(DemodulatorType.FILE)) {
			return Collections.singletonList(new BeaconInputStreamSource<>(rawIq, transmitter.getBeaconClass()));
		}
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		switch (baudRate) {
		case 200: {
			DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req, transmitter, baudRate);
			FskDemodulator byteInput = new FskDemodulator(source, 200, 1500.0f, 120, 200.0f);
			result.add(createBeaconSource(byteInput, req));
			break;
		}
		case 9600: {
			DopplerCorrectedSource source2 = new DopplerCorrectedSource(predict, rawIq, req, transmitter, baudRate);
			GmskDemodulator gmsk = new GmskDemodulator(source2, 9600, transmitter.getBandwidth(), 0.175f * 3);
			Set<String> codes = new HashSet<>();
			codes.add("0011111010");
			codes.add("1100000101");
			CorrelateSyncword correlate = new CorrelateSyncword(gmsk, 0, codes, HighSpeedFox.HIGH_SPEED_FRAME_SIZE * 10);
			result.add(new HighSpeedFox<>(correlate, Fox1DBeacon.class));
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + baudRate);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<Instrument> decodeImage(Satellite satellite, List<? extends Beacon> beacons) {
		Instrument camera = satellite.findFirstSeries();
		if (camera == null) {
			return Collections.emptyList();
		}
		List<PictureScanLine> scanLines = new ArrayList<>();
		for (Fox1DBeacon cur : (List<Fox1DBeacon>) beacons) {
			if (cur.getPictureScanLines() == null) {
				continue;
			}
			scanLines.addAll(cur.getPictureScanLines());
		}

		List<File> series = new ArrayList<>();
		int index = 0;
		FoxPictureDecoder pictureDecoder = new FoxPictureDecoder(scanLines);
		while (pictureDecoder.hasNext()) {
			BufferedImage image = pictureDecoder.next();
			if (image == null) {
				continue;
			}
			File imageFile = saveImage("fox1d-" + index + ".jpg", image);
			if (imageFile == null) {
				continue;
			}
			series.add(imageFile);
		}
		if (series.isEmpty()) {
			return Collections.emptyList();
		}
		Instrument result = new Instrument(camera);
		result.setImageSeries(series);
		return Collections.singletonList(result);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}

}
