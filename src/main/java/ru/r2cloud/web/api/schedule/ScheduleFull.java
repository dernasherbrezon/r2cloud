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

public class ScheduleFull extends AbstractHttpController {

    private final DeviceManager deviceManager;

    public ScheduleFull(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
    }

    @Override
    public ModelAndView doGet(IHTTPSession session) {
        JsonArray entity = new JsonArray();
        for (ObservationRequest cur : deviceManager.findScheduledObservations()) {
            JsonObject curRequest = new JsonObject();
            curRequest.add("id", cur.getId());
            curRequest.add("satelliteId", cur.getSatelliteId());
            curRequest.add("start", cur.getStartTimeMillis());
            curRequest.add("end", cur.getEndTimeMillis());
            entity.add(curRequest);
        }
        ModelAndView result = new ModelAndView();
        result.setData(entity);
        return result;
    }

    @Override
    public String getRequestMappingURL() {
        return "/api/v1/admin/schedule/full";
    }
}
