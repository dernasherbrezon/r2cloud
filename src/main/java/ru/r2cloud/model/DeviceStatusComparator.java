package ru.r2cloud.model;

import java.util.Comparator;

public class DeviceStatusComparator implements Comparator<DeviceStatus> {

	public static final DeviceStatusComparator INSTANCE = new DeviceStatusComparator();

	@Override
	public int compare(DeviceStatus o1, DeviceStatus o2) {
		return o1.getConfig().getId().compareTo(o2.getConfig().getId());
	}
}
