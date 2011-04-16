package org.webseer.transformation;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class InterfaceDefinition {

	URI id;

	String description;

	String name;

	Map<String, InputDefinition> inputs = new HashMap<String, InputDefinition>();

	Map<String, OutputDefinition> outputs = new HashMap<String, OutputDefinition>();

	public URI getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, InputDefinition> getInputs() {
		return inputs;
	}

	public Map<String, OutputDefinition> getOutputs() {
		return outputs;
	}

}