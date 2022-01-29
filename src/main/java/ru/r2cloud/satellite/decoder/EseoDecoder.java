package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.blocks.CorrelateSyncword;
import ru.r2cloud.jradio.blocks.SoftToHard;
import ru.r2cloud.jradio.eseo.Eseo;
import ru.r2cloud.jradio.eseo.EseoBeacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.Util;

public class EseoDecoder extends TelemetryDecoder {

	public EseoDecoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		int baudRate = req.getBaudRates().get(0);
		GmskDemodulator gmsk = new GmskDemodulator(source, baudRate, req.getBandwidth(), 0.175f * 3, 0.06f, Util.convertDecimation(baudRate), 2000);
		SoftToHard s2h = new SoftToHard(gmsk);
		CorrelateSyncword correlate = new CorrelateSyncword(s2h, 1, EseoBeacon.FLAG, 257 * 8);
		return new Eseo(correlate);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return EseoBeacon.class;
	}

}
