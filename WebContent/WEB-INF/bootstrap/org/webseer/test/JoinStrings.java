package org.webseer.test;

import org.webseer.java.FunctionDef;
import org.webseer.java.InputChannel;
import org.webseer.java.JavaFunction;
import org.webseer.java.OutputChannel;

@FunctionDef
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
