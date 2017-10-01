package ru.r2cloud;

import java.io.File;
import java.util.Comparator;

public class FilenameComparator implements Comparator<File> {

	public static final FilenameComparator INSTANCE_ASC = new FilenameComparator(true);
	public static final FilenameComparator INSTANCE_DESC = new FilenameComparator(false);

	private final boolean asc;

	private FilenameComparator(boolean asc) {
		this.asc = asc;
	}

	@Override
	public int compare(File o1, File o2) {
		if (asc) {
			return Long.valueOf(o1.getName()).compareTo(Long.valueOf(o2.getName()));
		} else {
			return Long.valueOf(o2.getName()).compareTo(Long.valueOf(o1.getName()));
		}
	}

}
