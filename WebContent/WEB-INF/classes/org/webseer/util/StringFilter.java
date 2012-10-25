package org.webseer.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.java.JavaFunction;

/**
 * Redirects a string by whether it matches a set of strings.
 * 
 * @author ryan
 */
public class StringFilter implements JavaFunction {

	@InputChannel
	private Iterator<String> toMatch;

	@InputChannel
	private String input;

	@OutputChannel
	private String matched;

	@OutputChannel
	private String notMatched;

	private Set<String> cached = null;

	@Override
	public void execute() throws Throwable {
		if (cached == null) {
			cached = new HashSet<String>();
			while (toMatch.hasNext()) {
				cached.add(toMatch.next());
			}
		}
		if (cached.contains(input)) {
			matched = input;
		} else {
			notMatched = input;
		}
	}

}
