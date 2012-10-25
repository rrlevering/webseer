package org.webseer.test;

import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.java.JavaFunction;

public class RemoveCharacters implements JavaFunction {

	@InputChannel
	public boolean works;

	@InputChannel
	public String originalString;

	@OutputChannel
	public String modifiedString;

	public void execute() {
		if (works) {
			if (originalString.length() > 3) {
				modifiedString = originalString.substring(0, originalString.length() - 3);
			} else {
				modifiedString = "";
			}
		} else {
			modifiedString = originalString;
		}
	}

}
