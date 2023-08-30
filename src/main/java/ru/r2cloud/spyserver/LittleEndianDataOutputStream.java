package ru.r2cloud.spyserver;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public class LittleEndianDataOutputStream implements DataOutput {

	private final DataOutputStream dis;

	public LittleEndianDataOutputStream(DataOutputStream dis) {
		this.dis = dis;
	}

	@Override
	public void write(int b) throws IOException {
		dis.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		dis.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		dis.write(b, off, len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeByte(int v) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeShort(int v) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeChar(int v) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeInt(int v) throws IOException {
		dis.write(0xFF & v);
		dis.write(0xFF & (v >> 8));
		dis.write(0xFF & (v >> 16));
		dis.write(0xFF & (v >> 24));
	}

	@Override
	public void writeLong(long v) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeFloat(float v) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeDouble(double v) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeBytes(String s) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeChars(String s) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeUTF(String s) throws IOException {
		// TODO Auto-generated method stub

	}

	public void flush() throws IOException {
		dis.flush();
	}

}
