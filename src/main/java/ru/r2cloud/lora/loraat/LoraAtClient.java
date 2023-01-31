package ru.r2cloud.lora.loraat;

import ru.r2cloud.lora.LoraObservationRequest;
import ru.r2cloud.lora.LoraResponse;
import ru.r2cloud.lora.LoraStatus;

public interface LoraAtClient {

	LoraStatus getStatus();

	LoraResponse startObservation(LoraObservationRequest loraRequest);

	LoraResponse stopObservation();

}