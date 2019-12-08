package ru.r2cloud.util;

import java.lang.reflect.Method;
import java.util.Comparator;

public class MethodComparator implements Comparator<Method> {
	
	public static final MethodComparator INSTANCE = new MethodComparator();

	@Override
	public int compare(Method o1, Method o2) {
		return o1.getName().compareTo(o2.getName());
	}

}
