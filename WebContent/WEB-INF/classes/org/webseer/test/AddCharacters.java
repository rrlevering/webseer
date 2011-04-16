package org.webseer.test;

import org.webseer.transformation.InputChannel;
import org.webseer.transformation.JavaFunction;
import org.webseer.transformation.OutputChannel;

public class AddCharacters implements JavaFunction {

	@InputChannel
	public String originalString;

	@OutputChannel
	public String modifiedString;

	public void execute() {
		modifiedString = originalString + "123";
	}

}
