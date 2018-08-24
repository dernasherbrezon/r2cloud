package ru.r2cloud;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilenameComparatorTest {
	
	@Test
	public void testSuccess() {
		List<File> files = new ArrayList<>();
		files.add(new File(".DS_Store"));
		files.add(new File("234"));
		files.add(new File("123"));
		Collections.sort(files, FilenameComparator.INSTANCE_ASC);
		
		assertEquals("123", files.get(0).getName());
		assertEquals("234", files.get(1).getName());
		assertEquals(".DS_Store", files.get(2).getName());
		
		Collections.sort(files, FilenameComparator.INSTANCE_DESC);
		
		assertEquals(".DS_Store", files.get(0).getName());
		assertEquals("234", files.get(1).getName());
		assertEquals("123", files.get(2).getName());
	}

}
