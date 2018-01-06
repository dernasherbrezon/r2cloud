package ru.r2cloud.it;

import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import ru.r2cloud.it.util.WebIT;

@RunWith(Suite.class)
@SuiteClasses({APTDecoderIT.class, WebIT.class})
public class IntegrationalTests {

	public static void main(String[] args) throws Exception {
		JUnitCore.main(IntegrationalTests.class.getName());
	}
	
}
