package org.webseer.java;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.webseer.bucket.Data;
import org.webseer.bucket.SimpleData;
import org.webseer.type.SimpleType;

public class JavaTypeTranslator {

	/**
	 * Converts data into a Java object of the target type, casting primitive values and throwing exceptions if the
	 * fields of the data don't match up with the fields of the target class.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T convertData(Data data, Class<T> targetClass) {
		if (data.getType().getName().equals("string")) {
			String string;
			try {
				string = new DataInputStream(data.getValue()).readUTF();
			} catch (IOException e) {
				throw new RuntimeException("Fix this IOException passing", e);
			}
			if (targetClass.equals(String.class)) {
				return (T) string;
			} else if (targetClass.equals(Integer.class) || targetClass.equals(Integer.TYPE)) {
				return (T) Integer.valueOf(string);
			} else if (targetClass.equals(Double.class) || targetClass.equals(Double.TYPE)) {
				return (T) Double.valueOf(string);
			}
		} else if (data.getType().getName().equals("int32")) {
			int integer;
			try {
				integer = new DataInputStream(data.getValue()).readInt();
			} catch (IOException e) {
				throw new RuntimeException("Fix this IOException passing", e);
			}
			if (targetClass.equals(String.class)) {
				return (T) String.valueOf(integer);
			} else if (targetClass.equals(Integer.class) || targetClass.equals(Integer.TYPE)) {
				return (T) Integer.valueOf(integer);
			} else if (targetClass.equals(Double.class) || targetClass.equals(Double.TYPE)) {
				return (T) Double.valueOf(integer);
			}
		}
		throw new RuntimeException("Conversion for " + data.getType().getName() + " to " + targetClass
				+ " not implemented yet");
	}

	public static Data convertObject(Object o) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		String typeName;
		try {
			if (o instanceof String) {
				new DataOutputStream(output).writeUTF((String) o);
				typeName = "string";
			} else if (o instanceof Integer) {
				new DataOutputStream(output).writeInt((Integer)o);
				typeName = "int32";
			} else {
				throw new RuntimeException("Conversion not implemented yet for " + o.getClass());
			}
		} catch (IOException e) {
			throw new RuntimeException("Fix this IOException passing", e);
		}
		return new SimpleData(output.toByteArray(), new SimpleType(typeName));
	}

	public static <T> Iterator<T> convertData(final Iterator<? extends Data> dataStream, final Class<T> targetClass) {
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				return dataStream.hasNext();
			}

			@Override
			public T next() {
				return JavaTypeTranslator.convertData(dataStream.next(), targetClass);
			}

			@Override
			public void remove() {
				dataStream.remove();
			}

		};
	}

	public static <T> Iterable<T> convertData(final Iterable<? extends Data> data, final Class<T> targetClass) {
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				// Convert the generic data into objects
				return convertData(data.iterator(), targetClass);
			}

		};
	}
}
