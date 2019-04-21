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

}
