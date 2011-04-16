package org.webseer.transformation;

import java.net.URI;

public class OutputDefinition {

	String description;

	public String getDescription() {
		return description;
	}

	public URI getModel() {
		return model;
	}

	URI model;
	public boolean multiple;
}