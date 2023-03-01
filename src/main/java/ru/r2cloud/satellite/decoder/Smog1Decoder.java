package ru.r2cloud.satellite.decoder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.demod.FskDemodulator;
import ru.r2cloud.jradio.smog1.Smog1;
import ru.r2cloud.jradio.smog1.Smog1Beacon;
import ru.r2cloud.jradio.smog1.Smog1RaCoded;
import ru.r2cloud.jradio.smog1.Smog1Short;
import ru.r2cloud.model.DemodulatorType;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.sdrmodem.SdrModemClient;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class Smog1Decoder extends TelemetryDecoder {

	public Smog1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public List<BeaconSource<? extends Beacon>> createBeaconSources(File rawIq, ObservationRequest req, final Transmitter transmitter, Integer baudRate) throws IOException {
		List<BeaconSource<? extends Beacon>> result = new ArrayList<>();
		result.add(new Smog1RaCoded(createDemodulator(baudRate, rawIq, req, transmitter), 128, 260));
		result.add(new Smog1RaCoded(createDemodulator(baudRate, rawIq, req, transmitter), 256, 514));
		result.add(new Smog1Short(createDemodulator(baudRate, rawIq, req, transmitter)));
		result.add(new Smog1(createDemodulator(baudRate, rawIq, req, transmitter)));
		return result;
	}

	private ByteInput createDemodulator(int baudRate, File rawIq, ObservationRequest req, final Transmitter transmitter) throws IOException {
		DemodulatorType type = config.getDemodulatorType(transmitter.getModulation());
		if (type.equals(DemodulatorType.SDRMODEM)) {
			return new SdrModemClient(config, rawIq, req, transmitter, baudRate);
		}
		DopplerCorrectedSource source = new DopplerCorrectedSource(predict, rawIq, req, transmitter);
		return new FskDemodulator(source, baudRate, 5000.0f, Util.convertDecimation(baudRate), 2000, true);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Smog1Beacon.class;
	}

}
