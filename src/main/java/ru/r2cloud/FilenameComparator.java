package ru.r2cloud;

import java.io.File;
import java.util.Comparator;

public class FilenameComparator implements Comparator<File> {

	public static final FilenameComparator INSTANCE = new FilenameComparator();

	@Override
	public int compare(File o1, File o2) {
		return Long.valueOf(o1.getName()).compareTo(Long.valueOf(o2.getName()));
	}

}
