package org.webseer.streams.model;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.User;

public class Workspace {

	private static final String NAME = "name";
	private static final String PUBLIC = "public";

	private final Node underlyingNode;

	public Workspace(GraphDatabaseService service, WorkspaceFactory factory, User owner, String name) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		if (factory.getWorkspace(name) != null) {
			throw new IllegalArgumentException("Name must be unique to the system");
		}
		underlyingNode.setProperty(NAME, name);
		setPublic(false);

		this.underlyingNode.createRelationshipTo(Neo4JUtils.getNode(owner), NeoRelationshipType.USER_WORKSPACE);
		factory.getUnderlyingNode().createRelationshipTo(this.underlyingNode,
				NeoRelationshipType.WORKSPACEFACTORY_WORKSPACE);
	}

	public Workspace(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public long getWorkspaceId() {
		return underlyingNode.getId();
	}

	public String getName() {
		return (String) underlyingNode.getProperty(NAME);
	}

	public void setName(String name) {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Must not be empty");
		}
		if (name.equals(getName())) {
			return;
		}
		if (getFactory().getWorkspace(name) != null) {
			throw new IllegalArgumentException("Name must be unique to the WorkspaceFactory");
		}
		underlyingNode.setProperty(NAME, name);
	}

	private WorkspaceFactory getFactory() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.WORKSPACEFACTORY_WORKSPACE,
				WorkspaceFactory.class);
	}

	public User getOwner() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.USER_WORKSPACE, User.class);
	}

	public Iterable<WorkspaceBucket> getWorkspaceBuckets() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.WORKSPACEBUCKET_WORKSPACE,
				WorkspaceBucket.class);
	}

	public boolean isPublic() {
		return ObjectUtils.equals(underlyingNode.getProperty(PUBLIC), Boolean.TRUE);
	}

	public void setPublic(boolean publik) {
		underlyingNode.setProperty(PUBLIC, publik);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public void delete() {
		// Delete all the buckets
		for (WorkspaceBucket bucket : getWorkspaceBuckets()) {
			bucket.delete();
		}
		for (Program program : getPrograms()) {
			program.delete();
		}

		underlyingNode.getSingleRelationship(NeoRelationshipType.USER_WORKSPACE, Direction.OUTGOING).delete();
		underlyingNode.getSingleRelationship(NeoRelationshipType.WORKSPACEFACTORY_WORKSPACE, Direction.INCOMING)
				.delete();

		underlyingNode.delete();
	}

	public WorkspaceBucket getWorkspaceBucket(String name) {
		for (WorkspaceBucket bucket : getWorkspaceBuckets()) {
			if (bucket.getName().equals(name)) {
				return bucket;
			}
		}
		return null;
	}

	public Iterable<Program> getPrograms() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.WORKSPACE_PROGRAM, Program.class);
	}

	public Program getProgram(String name) {
		for (Program program : getPrograms()) {
			if (program.getName().equals(name)) {
				return program;
			}
		}
		return null;
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Workspace)) {
			return false;
		}
		return ((Workspace) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "Workspace[" + getName() + "]";
	}

}
