package org.webseer.test;

import org.webseer.java.FunctionDef;
import org.webseer.java.InputChannel;
import org.webseer.java.JavaFunction;
import org.webseer.java.OutputChannel;

@FunctionDef
public class RemoveCharacters implements JavaFunction {

	@InputChannel
	public int numCharacters = 0;

	@InputChannel
	public String originalString;

	@OutputChannel
	public String modifiedString;

	public void execute() {
		if (numCharacters > 0) {
			if (originalString.length() > numChars) {
				modifiedString = originalString.substring(0, originalString.length() - numChars);
			} else {
				modifiedString = "";
			}
		} else {
			modifiedString = originalString;
		}
	}

}
