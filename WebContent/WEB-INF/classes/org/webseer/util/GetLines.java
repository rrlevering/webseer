package org.webseer.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.webseer.transformation.InputChannel;
import org.webseer.transformation.JavaFunction;
import org.webseer.transformation.OutputChannel;

import com.google.protobuf.ByteString;

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
