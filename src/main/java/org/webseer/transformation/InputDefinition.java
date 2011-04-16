package org.webseer.transformation;

import org.webseer.model.meta.InputType;

public class InputDefinition {

	InputType type;

	String description;

	boolean required;

	public InputType getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public boolean isRequired() {
		return required;
	}
}