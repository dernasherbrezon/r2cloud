package ru.r2cloud;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilenameComparatorTest {

	@Test
	public void testSuccess() {
		List<Path> files = new ArrayList<>();
		files.add(Path.of(".DS_Store"));
		files.add(Path.of("234"));
		files.add(Path.of("123"));
		Collections.sort(files, FilenameComparator.INSTANCE_ASC);

		assertEquals("123", files.get(0).getFileName().toString());
		assertEquals("234", files.get(1).getFileName().toString());
		assertEquals(".DS_Store", files.get(2).getFileName().toString());

		Collections.sort(files, FilenameComparator.INSTANCE_DESC);

		assertEquals(".DS_Store", files.get(0).getFileName().toString());
		assertEquals("234", files.get(1).getFileName().toString());
		assertEquals("123", files.get(2).getFileName().toString());
	}

	@Test
	public void testCompareNonDigit() {
		List<Path> files = new ArrayList<>();
		files.add(Path.of("a"));
		files.add(Path.of("b"));

		Collections.sort(files, FilenameComparator.INSTANCE_ASC);
		assertEquals("a", files.get(0).getFileName().toString());
		assertEquals("b", files.get(1).getFileName().toString());

		Collections.sort(files, FilenameComparator.INSTANCE_DESC);
		assertEquals("b", files.get(0).getFileName().toString());
		assertEquals("a", files.get(1).getFileName().toString());
	}

	@Test
	public void testCompareNonDigitWithDigit() {
		List<Path> files = new ArrayList<>();
		files.add(Path.of("1"));
		files.add(Path.of("b"));

		Collections.sort(files, FilenameComparator.INSTANCE_ASC);
		assertEquals("1", files.get(0).getFileName().toString());
		assertEquals("b", files.get(1).getFileName().toString());

		files = new ArrayList<>();
		files.add(Path.of("b"));
		files.add(Path.of("1"));
		Collections.sort(files, FilenameComparator.INSTANCE_DESC);
		assertEquals("b", files.get(0).getFileName().toString());
		assertEquals("1", files.get(1).getFileName().toString());
	}
}
