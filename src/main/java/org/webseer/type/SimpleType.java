package org.webseer.type;

public class SimpleType implements DataType {

	private final String name;

	public SimpleType(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
