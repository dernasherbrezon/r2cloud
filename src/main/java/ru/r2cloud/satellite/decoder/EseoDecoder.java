package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.ByteInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.eseo.Eseo;
import ru.r2cloud.jradio.eseo.EseoBeacon;
import ru.r2cloud.model.Observation;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class EseoDecoder extends TelemetryDecoder {

	public EseoDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(ByteInput demodulator, Observation req) {
		SoftToHard s2h = new SoftToHard(demodulator);
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 1, EseoBeacon.FLAG, 257 * 8);
		return new Eseo(correlate);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return EseoBeacon.class;
	}

}
