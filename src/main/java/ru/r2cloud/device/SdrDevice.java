package ru.r2cloud.device;

import ru.r2cloud.model.DeviceConfiguration;
import ru.r2cloud.model.DeviceStatus;
import ru.r2cloud.model.DeviceType;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.SdrStatus;
import ru.r2cloud.model.SdrType;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationFactory;
import ru.r2cloud.satellite.Schedule;
import ru.r2cloud.satellite.TransmitterFilter;
import ru.r2cloud.satellite.decoder.DecoderService;
import ru.r2cloud.satellite.reader.IQReader;
import ru.r2cloud.satellite.reader.PlutoSdrReader;
import ru.r2cloud.satellite.reader.RtlFmReader;
import ru.r2cloud.satellite.reader.RtlSdrReader;
import ru.r2cloud.satellite.reader.SdrServerReader;
import ru.r2cloud.sdr.SdrStatusDao;
import ru.r2cloud.util.Clock;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.util.ProcessFactory;
import ru.r2cloud.util.ThreadPoolFactory;

public class SdrDevice extends Device {

    private final Configuration config;
    private final ProcessFactory processFactory;
    private final SdrStatusDao statusDao;

    public SdrDevice(String id, TransmitterFilter filter, int numberOfConcurrentObservations, ObservationFactory observationFactory, ThreadPoolFactory threadpoolFactory, Clock clock, DeviceConfiguration deviceConfiguration, IObservationDao observationDao, DecoderService decoderService,
                     PredictOreKit predict, Schedule schedule, Configuration config, ProcessFactory processFactory) {
        super(id, filter, numberOfConcurrentObservations, observationFactory, threadpoolFactory, clock, deviceConfiguration, observationDao, decoderService, predict, schedule);
        this.config = config;
        this.processFactory = processFactory;
        this.statusDao = new SdrStatusDao(config, processFactory, deviceConfiguration.getRtlDeviceId());
    }

    @Override
    public IQReader createReader(ObservationRequest req, Transmitter transmitter) {
        switch (transmitter.getFraming()) {
            case APT:
                return new RtlFmReader(config, processFactory, req, transmitter);
            case LRPT:
            default:
                if (req.getSdrType().equals(SdrType.RTLSDR)) {
                    return new RtlSdrReader(config, processFactory, req);
                } else if (req.getSdrType().equals(SdrType.PLUTOSDR)) {
                    return new PlutoSdrReader(config, processFactory, req);
                } else if (req.getSdrType().equals(SdrType.SDRSERVER)) {
                    return new SdrServerReader(req);
                } else {
                    throw new IllegalArgumentException("unsupported sdr type: " + req.getSdrType());
                }
        }
    }

    @Override
    public DeviceStatus getStatus() {
        DeviceStatus result = super.getStatus();
        result.setType(DeviceType.SDR);
        SdrStatus status = statusDao.getStatus();
        result.setFailureMessage(status.getFailureMessage());
        result.setStatus(status.getStatus());
        result.setModel(status.getModel());
        return result;
    }

}
