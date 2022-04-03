package ru.r2cloud.satellite;

import ru.r2cloud.model.Transmitter;

public interface TransmitterFilter {

	boolean accept(Transmitter transmitter);

}
