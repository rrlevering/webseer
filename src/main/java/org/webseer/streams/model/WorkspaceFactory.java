package org.webseer.streams.model;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

public class WorkspaceFactory {

	private final Node underlyingNode;

	public WorkspaceFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public static WorkspaceFactory getWorkspaceFactory(GraphDatabaseService service) {
		return Neo4JUtils.getSingleton(service, WorkspaceFactory.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public Workspace getWorkspace(String name) {
		for (Workspace workspace : getAllWorkspaces()) {
			if (workspace.getName().equals(name)) {
				return workspace;
			}
		}
		return null;
	}

	public Iterable<Workspace> getAllWorkspaces() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.WORKSPACEFACTORY_WORKSPACE, Workspace.class);
	}

	public String toString() {
		return "WorkspaceFactory";
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof WorkspaceFactory)) {
			return false;
		}
		return ((WorkspaceFactory) o).getUnderlyingNode().equals(underlyingNode);
	}
}
