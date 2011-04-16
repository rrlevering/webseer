package org.webseer.web.beans;

import org.webseer.model.Program;

public class ProgramBean {

	private String name;
	private long id;
	private long graphId;

	public ProgramBean(Program program) {
		this.id = program.getProgramId();
		this.name = program.getName();
		this.graphId = program.getGraph().getGraphId();
	}

	public String getName() {
		return name;
	}

	public long getId() {
		return id;
	}

	public long getGraphId() {
		return graphId;
	}

}
