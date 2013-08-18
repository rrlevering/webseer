package org.webseer.transformation;

import java.util.Collections;
import java.util.Iterator;

import org.webseer.bucket.Data;
import org.webseer.java.JavaTypeTranslator;

public class InputReaders {

	private final static InputReader EMPTY_READER = new InputReader() {

		@Override
		public Iterator<Data> iterator() {
			return Collections.<Data> emptyList().iterator();
		}

	};

	public static InputReader getEmptyReader() {
		return EMPTY_READER;
	}

	public static InputReader getInputReader(final String string) {
		return new InputReader() {

			@Override
			public Iterator<Data> iterator() {
				return Collections.<Data> singleton(JavaTypeTranslator.convertObject(string)).iterator();
			}

		};
	}

	public static InputReader getInputReader(final Data inputObject) {
		return new InputReader() {

			@Override
			public Iterator<Data> iterator() {
				return Collections.singleton(inputObject).iterator();
			}

		};
	}

}
