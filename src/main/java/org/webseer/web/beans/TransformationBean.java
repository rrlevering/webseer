package org.webseer.web.beans;

import java.util.ArrayList;
import java.util.List;

import name.levering.ryan.util.IterableUtils;

import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;

public class TransformationBean {

	private InputBean[] inputs;

	private OutputBean[] outputs;

	private String label;

	private String uri;

	private String packageName;

	private String description;

	private boolean system;

	private boolean bucket;

	private long id;

	private Long version;

	public TransformationBean() {

	}

	public TransformationBean(Transformation transformation) {
		this.label = transformation.getSimpleName();
		this.uri = transformation.getName();
		this.packageName = transformation.getPackage();
		this.system = false;
		this.bucket = false;
		this.description = transformation.getDescription();
		this.id = Neo4JMetaUtils.getNode(transformation).getId();
		this.version = transformation.getSource().getVersion();
		this.inputs = new InputBean[IterableUtils.size(transformation.getInputPoints())];
		int i = 0;
		for (InputPoint input : transformation.getInputPoints()) {
			this.inputs[i++] = new InputBean(transformation, input);
		}
		this.outputs = new OutputBean[IterableUtils.size(transformation.getOutputPoints())];
		i = 0;
		for (OutputPoint output : transformation.getOutputPoints()) {
			this.outputs[i++] = new OutputBean(transformation, output);
		}
	}

	public Long getVersion() {
		return version;
	}

	public boolean isBucket() {
		return bucket;
	}

	public void setBucket(boolean bucket) {
		this.bucket = bucket;
	}

	public InputBean[] getInputs() {
		return inputs;
	}

	public String getPackage() {
		return packageName;
	}

	public List<InputBean> getMainInputs() {
		List<InputBean> mainInputs = new ArrayList<InputBean>();
		for (InputBean bean : getInputs()) {
			if (!bean.isConfiguration() && !bean.isSystem()) {
				mainInputs.add(bean);
			}
		}
		return mainInputs;
	}

	public void setInputs(InputBean[] inputs) {
		this.inputs = inputs;
	}

	public OutputBean[] getOutputs() {
		return outputs;
	}

	public void setOutputs(OutputBean[] outputs) {
		this.outputs = outputs;
	}

	public String getLabel() {
		return label;
	}

	public String getDescription() {
		return description;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public boolean isSystem() {
		return system;
	}

	public void setSystem(boolean system) {
		this.system = system;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}