package ru.r2cloud.rotator.allview;

import java.io.IOException;

public class AllviewMotor {

	private boolean fastMode;
	private int index;
	private AllviewClient client;
	private boolean stopped = false;
	private int currentSpeed = 0;
	private double currentDegree;
	private int cpr;
	private int timerInterruptFreq;

	public AllviewMotor(int index, AllviewClient client) {
		this.index = index;
		this.client = client;
	}

	public void setPosition(double degrees) throws IOException {
		client.sendMessage(":G" + index + "00\r");
		if (cpr == 0) {
			cpr = convertInteger(client.sendMessage(":a" + index + "\r"));
		}
		int number = (int) ((cpr / 360.0) * degrees) | 0x800000;
		String command = String.format(":S%d%02X%02X%02X\r", index, (number & 0xFF), ((number >> 8) & 0xFF), ((number >> 16) & 0xFF));
		client.sendMessage(command);
		client.sendMessage(":J" + index + "\r");
		stopped = false;
		fastMode = true;
		currentSpeed = 0;
	}

	public double getPosition() throws IOException {
		return convertAngle(client.sendMessage(":j" + index + "\r"));
	}

	private static int convertInteger(String inputStr) {
		int input = Integer.valueOf(inputStr, 16);
		return ((input & 0x0000FF) << 16) | (input & 0x00FF00) | ((input & 0xFF0000) >> 16);
	}

	private double convertAngle(String inputStr) {
		int converted = convertInteger(inputStr);
		boolean positive = true;
		if (converted > 0x800000) {
			converted = converted - 0x800000;
		} else {
			converted = 0x800000 - converted;
			positive = false;
		}
		double ticks_per_angle = (double) cpr / 360.0;
		double result = converted / ticks_per_angle;
		if (!positive) {
			return 360.0 - result;
		}
		return result;
	}

	public void stop() throws IOException {
		if (stopped) {
			return;
		}
		client.sendMessage(":K" + index + "\r");
	}

	public boolean waitMotorStop() throws IOException {
		if (stopped) {
			return stopped;
		}
		String status = client.sendMessage(":f" + index + "\r");
		int num = Integer.valueOf(status);
		if (((num >> 1) & 0x1) == 0) {
			stopped = true;
			currentSpeed = 0;
		}
		return stopped;
	}

	public boolean isStopped() {
		return stopped;
	}

	public boolean isFastMode() {
		return fastMode;
	}

	public void slew(double degreePerSec) throws IOException {
		boolean changeDirectionToCw = (degreePerSec > 0 && currentDegree < 0);
		boolean changeDirectionToCcw = (degreePerSec < 0 && currentDegree > 0);
		boolean changeDirectionNeeded = (changeDirectionToCw || changeDirectionToCcw);
		double degreeAbs = Math.abs(degreePerSec);
		int number = (int) ((getTimerInterruptFreq() * 360.0f) / degreeAbs / cpr);
		if (currentSpeed != 0 && currentSpeed == number && !changeDirectionNeeded) {
			return;
		}
		currentDegree = degreePerSec;
		currentSpeed = number;
		if (degreeAbs > 0.37037037037037) {
			// slow mode to fast mode
			if (stopped) {
				int direction = degreePerSec > 0 ? 0 : 1;
				client.sendMessage(":G" + index + "3" + direction + "\r");
			}
			number = 32 * number;
			String command = String.format(":I%d%02X%02X%02X\r", index, (number & 0xFF), ((number >> 8) & 0xFF), ((number >> 16) & 0xFF));
			client.sendMessage(command);
			client.sendMessage(":J" + index + "\r");
			fastMode = true;
		} else {
			if (stopped) {
				int direction = degreePerSec > 0 ? 0 : 1;
				client.sendMessage(":G" + index + "1" + direction + "\r");
			}
			String command = String.format(":I%d%02X%02X%02X\r", index, (number & 0xFF), ((number >> 8) & 0xFF), ((number >> 16) & 0xFF));
			client.sendMessage(command);
			if (stopped) {
				// switch from fast to slow mode require full stop. thus start motor command
				client.sendMessage(":J" + index + "\r");
			}
			fastMode = false;
		}
		stopped = false;
	}

	public boolean isStopRequired(double degree) throws IOException {
		if (stopped) {
			return false;
		}
		// transition from slow to fast
		if (!fastMode && Math.abs(degree) > 0.37037037037037) {
			return true;
		}
		if (fastMode) {
			// transition from fast to slow
			if (Math.abs(degree) < 0.37037037037037) {
				return true;
			}
			// only when required speed is actually changed
			int number = (int) ((getTimerInterruptFreq() * 360.0f) / Math.abs(degree) / cpr);
			if (currentSpeed != number) {
				return true;
			}
		}
		boolean changeDirectionToCw = (degree > 0 && currentDegree < 0);
		boolean changeDirectionToCcw = (degree < 0 && currentDegree > 0);
		if (changeDirectionToCw || changeDirectionToCcw) {
			return true;
		}

		return false;
	}

	private int getTimerInterruptFreq() throws IOException {
		if (timerInterruptFreq == 0) {
			timerInterruptFreq = convertInteger(client.sendMessage(":b1\r"));
		}
		return timerInterruptFreq;
	}

}
