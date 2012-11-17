package org.webseer.transformation;

import java.util.Collections;
import java.util.Iterator;

public class InputReaders {

	private final static InputReader EMPTY_READER = new InputReader() {

		@Override
		public Iterator<Object> getInputStream() {
			return Collections.<Object>emptyList().iterator();
		}

	};


	public static InputReader getEmptyReader() {
		return EMPTY_READER;
	}


	public static InputReader getInputReader(final Object inputObject) {
		return new InputReader() {

			@Override
			public Iterator<Object> getInputStream() {
				return Collections.singleton(inputObject).iterator();
			}
			
		};
	}
	
}
