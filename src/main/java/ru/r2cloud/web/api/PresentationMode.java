package ru.r2cloud.web.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import ru.r2cloud.ObservationFullComparator;
import ru.r2cloud.device.DeviceManager;
import ru.r2cloud.model.Observation;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.model.Satellite;
import ru.r2cloud.model.Transmitter;
import ru.r2cloud.satellite.IObservationDao;
import ru.r2cloud.satellite.ObservationRequestComparator;
import ru.r2cloud.satellite.SatelliteDao;
import ru.r2cloud.util.Configuration;
import ru.r2cloud.web.AbstractHttpController;
import ru.r2cloud.web.ModelAndView;

public class PresentationMode extends AbstractHttpController {

    private final SatelliteDao dao;
    private final IObservationDao resultDao;
    private final Configuration config;
    private final DeviceManager deviceManager;

    public PresentationMode(Configuration config, SatelliteDao dao, IObservationDao resultDao, DeviceManager deviceManager) {
        this.dao = dao;
        this.resultDao = resultDao;
        this.config = config;
        this.deviceManager = deviceManager;
    }

    @Override
    public ModelAndView doGet(IHTTPSession session) {
        boolean enabled = config.getBoolean("presentationMode");
        ModelAndView result = new ModelAndView();
        if (!enabled) {
            result.setStatus(Response.Status.UNAUTHORIZED);
            result.setData("{}");
            return result;
        }
        List<Satellite> all = dao.findAll();
        List<Observation> observations = new ArrayList<>();
        List<ObservationRequest> requests = new ArrayList<>();
        for (Satellite cur : all) {
            observations.addAll(resultDao.findAllBySatelliteId(cur.getId()));
            for (Transmitter curTransmitter : cur.getTransmitters()) {
                ObservationRequest nextObservation = deviceManager.findFirstByTransmitter(curTransmitter);
                if (nextObservation == null) {
                    continue;
                }
                requests.add(nextObservation);
            }
        }
        Collections.sort(requests, ObservationRequestComparator.INSTANCE);
        JsonArray jsonObservations = new JsonArray();
        for (int i = 0; i < 5 && i < requests.size(); i++) {
            ObservationRequest cur = requests.get(i);
            Satellite curSatellite = dao.findById(cur.getSatelliteId());
            JsonObject curRequest = new JsonObject();
            curRequest.add("id", cur.getId());
            curRequest.add("name", curSatellite.getName());
            curRequest.add("satelliteId", cur.getSatelliteId());
            curRequest.add("tle", cur.getTle().toJson());
            curRequest.add("start", cur.getStartTimeMillis());
            curRequest.add("end", cur.getEndTimeMillis());
            curRequest.add("status", "NEW");
            curRequest.add("numberOfDecodedPackets", 0);
            curRequest.add("hasData", false);
            jsonObservations.add(curRequest);
        }

        Collections.sort(observations, ObservationFullComparator.INSTANCE);
        for (int i = 0; i < 5 && i < observations.size(); i++) {
            Observation cur = observations.get(i);
            Satellite curSatellite = dao.findById(cur.getSatelliteId());
            if (curSatellite == null || cur.getTle() == null) {
                continue;
            }
            JsonObject curObservation = new JsonObject();
            curObservation.add("id", cur.getId());
            curObservation.add("name", curSatellite.getName());
            curObservation.add("satelliteId", cur.getSatelliteId());
            curObservation.add("tle", cur.getTle().toJson());
            curObservation.add("start", cur.getStartTimeMillis());
            curObservation.add("end", cur.getEndTimeMillis());
            curObservation.add("status", cur.getStatus().name());
            curObservation.add("numberOfDecodedPackets", cur.getNumberOfDecodedPackets());
            curObservation.add("hasData", cur.hasData());
            jsonObservations.add(curObservation);
        }

        JsonObject baseStation = new JsonObject();
        baseStation.add("lat", config.getDouble("locaiton.lat"));
        baseStation.add("lng", config.getDouble("locaiton.lon"));

        JsonObject obj = new JsonObject();
        obj.add("observations", jsonObservations);
        obj.add("basestation", baseStation);

        result.setData(obj.toString());
        return result;
    }

    @Override
    public String getRequestMappingURL() {
        return "/api/v1/presentationMode";
    }

}
