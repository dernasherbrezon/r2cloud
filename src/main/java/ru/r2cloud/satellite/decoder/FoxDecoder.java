package ru.r2cloud.satellite.decoder;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconInputStream;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.fox.Fox1DBeacon;
import ru.r2cloud.jradio.fox.FoxPictureDecoder;
import ru.r2cloud.jradio.fox.HighSpeedFox;
import ru.r2cloud.jradio.fox.PictureScanLine;
import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class FoxDecoder<T extends Beacon> extends FoxSlowDecoder<T> {

	private static final Logger LOG = LoggerFactory.getLogger(FoxDecoder.class);

	private final Class<T> clazz;

	public FoxDecoder(PredictOreKit predict, Configuration config, Class<T> clazz) {
		super(predict, config, clazz);
		this.clazz = clazz;
	}

	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req) throws IOException {
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		// slow fox
		DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req);
		result.add(createBeaconSource(source, req));

		DopplerCorrectedSource source2 = new DopplerCorrectedSource(predict, rawIq, req);
		GmskDemodulator gmsk = new GmskDemodulator(source2, 9600, req.getBandwidth(), 0.175f * 3);
		SoftToHard s2h = new SoftToHard(gmsk);
		Set<String> codes = new HashSet<>();
		codes.add("0011111010");
		codes.add("1100000101");
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 0, codes, HighSpeedFox.HIGH_SPEED_FRAME_SIZE * 10);
		result.add(new HighSpeedFox<>(correlate, Fox1DBeacon.class));

		return result;
	}

	@Override
	public DecoderResult decode(File rawIq, ObservationRequest req) {
		DecoderResult result = super.decode(rawIq, req);
		if (result.getDataPath() != null) {
			try (BeaconInputStream<Fox1DBeacon> bis = new BeaconInputStream<>(new BufferedInputStream(new FileInputStream(result.getDataPath())), Fox1DBeacon.class)) {
				List<PictureScanLine> scanLines = new ArrayList<>();
				while (bis.hasNext()) {
					Fox1DBeacon beacon = bis.next();
					if (beacon.getPictureScanLines() == null) {
						continue;
					}
					scanLines.addAll(beacon.getPictureScanLines());
				}
				FoxPictureDecoder pictureDecoder = new FoxPictureDecoder(scanLines);
				while (pictureDecoder.hasNext()) {
					BufferedImage cur = pictureDecoder.next();
					if (cur == null) {
						continue;
					}
					File imageFile = saveImage("fox1d-" + req.getId() + ".jpg", cur);
					if (imageFile != null) {
						result.setImagePath(imageFile);
						// interested only in the first image
						break;
					}
				}
			} catch (IOException e) {
				LOG.error("unable to read data", e);
			}
		}
		return result;
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return clazz;
	}

}
