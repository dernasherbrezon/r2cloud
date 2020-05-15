package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Ax25G3ruhBeaconSource;
import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.qarman.QarmanBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class QarmanDecoder extends TelemetryDecoder {

	public QarmanDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		GmskDemodulator demodulator = new GmskDemodulator(source, 9600, req.getBandwidth(), 0.175f * 3, 0.02f, 1, 2000);
		return new Ax25G3ruhBeaconSource<>(demodulator, QarmanBeacon.class);
	}

	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return QarmanBeacon.class;
	}
}
