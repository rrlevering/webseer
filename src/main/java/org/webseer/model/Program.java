package org.webseer.model;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.program.Neo4JProgramUtils;
import org.webseer.model.program.TransformationGraph;

public class Program {

	private static final String NAME = "name";

	private final Node underlyingNode;

	public Program(GraphDatabaseService service, Workspace workspace, TransformationGraph graph, String name) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		if (workspace.getProgram(name) != null) {
			throw new IllegalArgumentException("Name must be unique to the workspace");
		}
		underlyingNode.setProperty(NAME, name);
		this.underlyingNode.createRelationshipTo(Neo4JProgramUtils.getNode(graph), NeoRelationshipType.PROGRAM_GRAPH);
		this.underlyingNode.createRelationshipTo(workspace.getUnderlyingNode(), NeoRelationshipType.WORKSPACE_PROGRAM);
	}

	public Program(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public TransformationGraph getGraph() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.PROGRAM_GRAPH, TransformationGraph.class);
	}

	public void setName(String name) {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Can't be empty");
		}
		if (name.equals(getName())) {
			return;
		}
		Workspace workspace = getWorkspace();
		if (workspace.getProgram(name) != null) {
			throw new IllegalArgumentException("Name must be unique to the bucket");
		}
		underlyingNode.setProperty(NAME, name);
	}

	public Workspace getWorkspace() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.WORKSPACE_PROGRAM, Workspace.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public String getName() {
		return (String) underlyingNode.getProperty(NAME);
	}

	public void delete() {
		getGraph().delete();
		underlyingNode.getSingleRelationship(NeoRelationshipType.PROGRAM_GRAPH, Direction.OUTGOING).delete();
		underlyingNode.getSingleRelationship(NeoRelationshipType.WORKSPACE_PROGRAM, Direction.OUTGOING).delete();
		underlyingNode.delete();
	}

	public long getProgramId() {
		return underlyingNode.getId();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Program)) {
			return false;
		}
		return ((Program) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "Program[" + getName() + "]";
	}

}
