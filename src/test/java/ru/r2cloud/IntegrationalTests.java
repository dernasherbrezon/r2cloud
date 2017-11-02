package ru.r2cloud;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import ru.r2cloud.satellite.SimpleTestIT;

@RunWith(Suite.class)
@SuiteClasses({SimpleTestIT.class})
public class IntegrationalTests {

	public static void main(String[] args) throws Exception {
		JUnitCore.main(IntegrationalTests.class.getName());
	}
	
}
