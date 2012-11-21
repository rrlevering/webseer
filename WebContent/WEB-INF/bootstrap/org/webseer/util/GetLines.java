package org.webseer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.webseer.java.FunctionDef;
import org.webseer.java.InputChannel;
import org.webseer.java.JavaFunction;
import org.webseer.java.OutputChannel;

import com.google.protobuf.ByteString;

@FunctionDef
public class GetLines implements JavaFunction {

	@OutputChannel
	public Iterable<String> lines;

	@InputChannel
	public ByteString file;

	@SuppressWarnings("unchecked")
	@Override
	public void execute() {
		try {
			lines = IOUtils.readLines(new ByteArrayInputStream(file.toByteArray()));
		} catch (IOException e) {
			// Do nothing
		}
	}
}
