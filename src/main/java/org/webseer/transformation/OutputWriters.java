package org.webseer.transformation;

import java.util.Collection;

public class OutputWriters {

	public static <T> OutputWriter<T> getOutputWriter(final Collection<T> collector) {
		return new OutputWriter<T>() {

			@Override
			public void writeObject(T o) {
				collector.add(o);
			}
			
		};
	}
	
}
