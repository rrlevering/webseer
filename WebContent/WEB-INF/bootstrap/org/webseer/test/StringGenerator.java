package org.webseer.test;

import org.webseer.java.FunctionDef;
import org.webseer.java.JavaFunction;
import org.webseer.java.OutputChannel;

/**
 * This is just a "test" class to seed webseer with some starting functions.
 * 
 * @author ryan
 */
@FunctionDef
public class StringGenerator implements JavaFunction {

	@OutputChannel
	public String generatedString;

	public void execute() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		generatedString = String.valueOf("String from time " + System.currentTimeMillis());
	}

}