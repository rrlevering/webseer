package org.webseer.test;

import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.java.JavaFunction;

public class JoinStrings implements JavaFunction {

	@InputChannel
	public String string1;

	@InputChannel
	public String string2;

	@OutputChannel
	public String joinedString;

	@Override
	public void execute() {
		joinedString = string1 + string2;
	}

}
