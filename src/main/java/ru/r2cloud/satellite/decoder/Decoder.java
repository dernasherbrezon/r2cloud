package ru.r2cloud.satellite.decoder;

import java.io.File;

import ru.r2cloud.model.DecoderResult;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;

public interface Decoder {

	DecoderResult decode(final File rawFile, final Observation request, final Transmitter transmitter, final Satellite satellite);

}
