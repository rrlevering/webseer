package org.webseer.web.beans;

import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.InputType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Transformation;

public class InputBean {

	private long inputPointId;

	private long nodeId;

	private String label;

	private boolean configuration;

	private boolean system;

	private String value;

	private String type;

	private boolean multiple;

	private boolean varargs;

	public InputBean() {

	}

	public InputBean(Transformation transformation, InputPoint input) {
		this.inputPointId = input.getInputPointId();
		this.nodeId = Neo4JMetaUtils.getNode(transformation).getId();
		this.label = input.getName();
		this.configuration = input.getInputType() == InputType.CONFIGURATION;
		this.system = false;
		this.type = input.getType().getName();
		this.multiple = input.getInputType() == InputType.AGGREGATE;
		this.varargs = input.isVarArgs();
	}

	public boolean isVarargs() {
		return varargs;
	}

	public boolean isMultiple() {
		return multiple;
	}

	public long getInputPointId() {
		return inputPointId;
	}

	public void setInputPointId(long inputPointId) {
		this.inputPointId = inputPointId;
	}

	public String getType() {
		return type;
	}

	public long getNodeId() {
		return nodeId;
	}

	public void setNodeId(long nodeId) {
		this.nodeId = nodeId;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isConfiguration() {
		return configuration;
	}

	public void setConfiguration(boolean configuration) {
		this.configuration = configuration;
	}

	public boolean isSystem() {
		return system;
	}
}