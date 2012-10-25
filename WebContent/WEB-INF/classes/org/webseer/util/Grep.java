package org.webseer.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.webseer.transformation.InputChannel;
import org.webseer.transformation.ItemOutputStream;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.java.JavaFunction;

/**
 * Returns lines that match a certain regex pattern.
 */
public class Grep implements JavaFunction {

	@InputChannel
	private InputStream file;

	@InputChannel
	private String pattern;

	@OutputChannel
	public ItemOutputStream<String> lines;

	@Override
	public void execute() throws Throwable {
		Pattern patternObject = Pattern.compile(pattern);
		BufferedReader reader = new BufferedReader(new InputStreamReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			Matcher matcher = patternObject.matcher(line);
			if (matcher.find()) {
				if (matcher.groupCount() > 0) {
					lines.writeObject(matcher.group(1));
				} else {
					lines.writeObject(line);
				}
			}
		}
	}

}
