package ru.r2cloud;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SampleClass {

	private int f1 = 1;
	private short f2 = 2;
	private byte f3 = 3;
	private long f4 = 4;
	private float f5 = 5.1f;
	private double f6 = 6.1;
	private String f7 = "f7";
	private List<String> f8 = Arrays.asList("1", "2", "3");
	private Map<String, Object> f9 = Collections.singletonMap("f9", Arrays.asList("1", "2", "3"));
	private SampleEnum f10 = SampleEnum.E2;
	private double[] f11 = new double[] { 1.1, 2.2, 3.3 };
	private Object f12 = null;
	private Object f13 = new Object();
	private List<Object> f14 = Arrays.asList(new Object());
	private Map<String, Object> f15 = Collections.singletonMap("f15", new Object());
	private Object[] f16 = new Object[] { new Object() };
	private byte[] f17 = new byte[] { 1, 2, 3 };

	public byte[] getF17() {
		return f17;
	}
	
	public void setF17(byte[] f17) {
		this.f17 = f17;
	}
	
	public Object[] getF16() {
		return f16;
	}

	public void setF16(Object[] f16) {
		this.f16 = f16;
	}

	public Map<String, Object> getF15() {
		return f15;
	}

	public void setF15(Map<String, Object> f15) {
		this.f15 = f15;
	}

	public List<Object> getF14() {
		return f14;
	}

	public void setF14(List<Object> f14) {
		this.f14 = f14;
	}

	public Object getF13() {
		return f13;
	}

	public void setF13(Object f13) {
		this.f13 = f13;
	}

	public Object getFailing() {
		throw new IllegalArgumentException("expected illegal argument");
	}

	public Object getF12() {
		return f12;
	}

	public void setF12(Object f12) {
		this.f12 = f12;
	}

	public int getF1() {
		return f1;
	}

	public void setF1(int f1) {
		this.f1 = f1;
	}

	public short getF2() {
		return f2;
	}

	public void setF2(short f2) {
		this.f2 = f2;
	}

	public byte getF3() {
		return f3;
	}

	public void setF3(byte f3) {
		this.f3 = f3;
	}

	public long getF4() {
		return f4;
	}

	public void setF4(long f4) {
		this.f4 = f4;
	}

	public float getF5() {
		return f5;
	}

	public void setF5(float f5) {
		this.f5 = f5;
	}

	public double getF6() {
		return f6;
	}

	public void setF6(double f6) {
		this.f6 = f6;
	}

	public String getF7() {
		return f7;
	}

	public void setF7(String f7) {
		this.f7 = f7;
	}

	public List<String> getF8() {
		return f8;
	}

	public void setF8(List<String> f8) {
		this.f8 = f8;
	}

	public Map<String, Object> getF9() {
		return f9;
	}

	public void setF9(Map<String, Object> f9) {
		this.f9 = f9;
	}

	public SampleEnum getF10() {
		return f10;
	}

	public void setF10(SampleEnum f10) {
		this.f10 = f10;
	}

	public double[] getF11() {
		return f11;
	}

	public void setF11(double[] f11) {
		this.f11 = f11;
	}

}
