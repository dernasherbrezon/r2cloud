package ru.r2cloud.lora.loraat.gatt;

public class LoraAtDeviceStatus {

	private int bluetoothRssi;
	private int sx127xRawTemperature;
	private int solarVoltage;
	private int solarCurrent;
	private int batteryVoltage;
	private int batteryCurrent;

	public int getBluetoothRssi() {
		return bluetoothRssi;
	}

	public void setBluetoothRssi(int bluetoothRssi) {
		this.bluetoothRssi = bluetoothRssi;
	}

	public int getSx127xRawTemperature() {
		return sx127xRawTemperature;
	}

	public void setSx127xRawTemperature(int sx127xRawTemperature) {
		this.sx127xRawTemperature = sx127xRawTemperature;
	}

	public int getSolarVoltage() {
		return solarVoltage;
	}

	public void setSolarVoltage(int solarVoltage) {
		this.solarVoltage = solarVoltage;
	}

	public int getSolarCurrent() {
		return solarCurrent;
	}

	public void setSolarCurrent(int solarCurrent) {
		this.solarCurrent = solarCurrent;
	}

	public int getBatteryVoltage() {
		return batteryVoltage;
	}

	public void setBatteryVoltage(int batteryVoltage) {
		this.batteryVoltage = batteryVoltage;
	}

	public int getBatteryCurrent() {
		return batteryCurrent;
	}

	public void setBatteryCurrent(int batteryCurrent) {
		this.batteryCurrent = batteryCurrent;
	}

}
