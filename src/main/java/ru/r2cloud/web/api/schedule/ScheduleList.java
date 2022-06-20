package ru.r2cloud.web.api.schedule;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class ScheduleList extends AbstractHttpController {

    private final SatelliteDao satelliteDao;
    private final DeviceManager deviceManager;

    public ScheduleList(SatelliteDao satelliteDao, DeviceManager deviceManager) {
        this.satelliteDao = satelliteDao;
        this.deviceManager = deviceManager;
    }

    @Override
    public ModelAndView doGet(IHTTPSession session) {
        JsonArray entity = new JsonArray();
        for (Satellite cur : satelliteDao.findAll()) {
            JsonObject curSatellite = new JsonObject();
            curSatellite.add("id", cur.getId());
            curSatellite.add("name", cur.getName());
            curSatellite.add("enabled", cur.isEnabled());
            Transmitter transmitter = null;
            for (Transmitter curTransmitter : cur.getTransmitters()) {
                ObservationRequest nextObservation = deviceManager.findFirstByTransmitter(curTransmitter);
                if (transmitter == null) {
                    transmitter = curTransmitter;
                }
                if (nextObservation == null) {
                    continue;
                }
                // override transmitter for the schedule pass
                transmitter = curTransmitter;
                curSatellite.add("nextPass", nextObservation.getStartTimeMillis());
                break;
            }
            if (transmitter != null) {
                curSatellite.add("frequency", transmitter.getFrequency());
            }
            entity.add(curSatellite);
        }
        ModelAndView result = new ModelAndView();
        result.setData(entity);
        return result;
    }

    @Override
    public String getRequestMappingURL() {
        return "/api/v1/admin/schedule/list";
    }
}
