package org.webseer.test;

import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.java.JavaFunction;

/**
 * This is just a "test" class to seed webseer with some starting functions.
 * 
 * @author ryan
 */
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