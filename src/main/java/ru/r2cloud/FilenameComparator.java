package ru.r2cloud;

import java.nio.file.Path;
import java.util.Comparator;

public class FilenameComparator implements Comparator<Path> {

	public static final FilenameComparator INSTANCE_ASC = new FilenameComparator(true);
	public static final FilenameComparator INSTANCE_DESC = new FilenameComparator(false);

	private final boolean asc;

	private FilenameComparator(boolean asc) {
		this.asc = asc;
	}

	@SuppressWarnings("null")
	@Override
	public int compare(Path o1, Path o2) {
		Long n1 = convert(o1);
		Long n2 = convert(o2);
		if( n1 == null && n2 != null ) {
			if( asc ) {
				return 1;
			} else {
				return -1;
			}
		}
		if( n1 != null && n2 == null ) {
			if( asc ) {
				return -1;
			} else {
				return 1;
			}
		}
		if( n1 == null && n2 == null ) {
			return 0;
		}
		if (asc) {
			return n1.compareTo(n2);
		} else {
			return n2.compareTo(n1);
		}
	}

	private static Long convert(Path file) {
		try {
			return Long.valueOf(file.getFileName().toString());
		} catch (Exception e) {
			return null;
		}
	}

}
