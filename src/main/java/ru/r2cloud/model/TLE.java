package ru.r2cloud.model;

import java.util.Arrays;

public class TLE extends uk.me.g4dpz.satellite.TLE {

	private static final long serialVersionUID = 6446052218267434768L;

	private final String[] raw;

	public TLE(String[] tle) throws IllegalArgumentException {
		super(tle);
		this.raw = tle;
	}

	public String[] getRaw() {
		return raw;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(raw);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TLE other = (TLE) obj;
		if (!Arrays.equals(raw, other.raw))
			return false;
		return true;
	}
	
}
