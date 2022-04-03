package ru.r2cloud.satellite.decoder;

import java.io.File;

import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.model.DecoderResult;

public interface Decoder {

	DecoderResult decode(final File rawFile, final ObservationRequest request, final Transmitter transmitter);

}
