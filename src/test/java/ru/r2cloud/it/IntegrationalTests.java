package ru.r2cloud.it;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ObservationIT.class, WebIT.class})
public class IntegrationalTests {

	public static void main(String[] args) throws Exception {
		JUnitCore.main(IntegrationalTests.class.getName());
	}
	
}
