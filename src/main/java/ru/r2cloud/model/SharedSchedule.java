package ru.r2cloud.model;

import ru.r2cloud.satellite.Schedule;

import java.util.HashSet;
import java.util.Set;

public class SharedSchedule {

    private Schedule schedule;
    private Set<String> devicesIds = new HashSet<>();

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public Set<String> getDevicesIds() {
        return devicesIds;
    }

    public void setDevicesIds(Set<String> devicesIds) {
        this.devicesIds = devicesIds;
    }
}
