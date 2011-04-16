package org.webseer.model;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;

public class WorkspaceFactory {

	private final Node underlyingNode;

	public WorkspaceFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public static WorkspaceFactory getWorkspaceFactory(NeoService service) {
		return Neo4JUtils.getSingleton(service, NeoRelationshipType.REFERENCE_WORKSPACEFACTORY, WorkspaceFactory.class);
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
