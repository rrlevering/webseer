package org.webseer.bucket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.webseer.model.meta.Field;
import org.webseer.type.DataType;

public class SimpleData implements Data {

	private final byte[] data;
	private final DataType type;

	public SimpleData(byte[] data, DataType type) {
		this.data = data;
		this.type = type;
	}
	
	@Override
	public DataType getType() {
		return this.type;
	}

	@Override
	public Data getField(Field field) {
		return null;
	}

	@Override
	public InputStream getValue() {
		return new ByteArrayInputStream(data);
	}


}
