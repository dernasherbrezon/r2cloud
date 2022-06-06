package ru.r2cloud.sdrmodem;

public enum MessageType {

	RX_REQUEST(0), SHUTDOWN(1), PING(3), TX_DATA(4), TX_REQUEST(5), RESPONSE(2);

	private final int code;

	private MessageType(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static MessageType valueOfCode(int code) {
		for (MessageType cur : values()) {
			if (cur.code == code) {
				return cur;
			}
		}
		return null;
	}

}
