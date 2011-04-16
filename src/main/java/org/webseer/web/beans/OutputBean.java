package org.webseer.web.beans;

import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.transformation.OutputDefinition;

public class OutputBean {

	private long outputPointId;

	private long nodeId;

	private String label;

	private String type;

	public OutputBean() {

	}

	public OutputBean(Transformation transformation, OutputPoint output) {
		this.outputPointId = output.getOutputPointId();
		this.nodeId = Neo4JMetaUtils.getNode(transformation).getId();
		this.label = output.getName();
		this.type = output.getType().getName();
	}

	public OutputBean(String key, OutputDefinition value) {
		this.outputPointId = -1;
		this.nodeId = -1;
		this.label = key;
		this.type = value.getModel().toString();
	}

	public long getOutputPointId() {
		return outputPointId;
	}

	public void setOutputPointId(long outputPointId) {
		this.outputPointId = outputPointId;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}