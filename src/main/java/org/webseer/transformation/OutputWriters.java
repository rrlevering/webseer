package org.webseer.transformation;

import java.util.Collection;

import org.webseer.bucket.Data;
import org.webseer.java.JavaTypeTranslator;

public class OutputWriters {

	public static OutputWriter getStringWriter(final Collection<String> collector) {
		return new OutputWriter() {

			@Override
			public void writeData(Data data) {
				collector.add(JavaTypeTranslator.convertData(data, String.class));
			}

		};
	}

}
