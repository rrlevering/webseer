package org.webseer.test;

import org.webseer.java.FunctionDef;
import org.webseer.java.InputChannel;
import org.webseer.java.JavaFunction;
import org.webseer.java.OutputChannel;

@FunctionDef(description = "Test transformation that adds some characters to a string")
public class AddCharacters implements JavaFunction {

	@InputChannel
	public String originalString;

	@OutputChannel
	public String modifiedString;

	public void execute() {
		modifiedString = originalString + "123";
	}

}
