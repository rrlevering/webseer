package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * Represents an abstraction of a library, a set of generally related code that is versioned together.
 * 
 * @author Ryan Levering
 */
public class Library {

	private static final String VERSION = "version";

	private final static String NAME = "name";

	private final static String GROUP = "group";

	private final static String DATA = "data";

	private final Node underlyingNode;

	public Library(GraphDatabaseService service, String group, String libraryName, String version, byte[] data) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		Neo4JUtils.setProperty(underlyingNode, NAME, libraryName);
		Neo4JUtils.setProperty(underlyingNode, GROUP, group);
		Neo4JUtils.setProperty(underlyingNode, VERSION, version);
		Neo4JUtils.setProperty(underlyingNode, DATA, data);
	}

	public Library(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public String getGroup() {
		return Neo4JUtils.getString(underlyingNode, GROUP);
	}

	public String getName() {
		return Neo4JUtils.getString(underlyingNode, NAME);
	}

	public String getVersion() {
		return Neo4JUtils.getString(underlyingNode, VERSION);
	}

	public byte[] getData() {
		return Neo4JUtils.getByteArray(underlyingNode, DATA);
	}
	
	public Iterable<LibraryResource> getResources() {
		return Neo4JUtils
				.getIterable(underlyingNode, NeoRelationshipType.LIBRARY_RESOURCE, LibraryResource.class);
	}
	
	public LibraryResource getResource(String name) {
		for (LibraryResource resource : getResources()) {
			if (resource.getName().equals(name)) {
				return resource;
			}
		}
		return null;
	}

	Node getUnderlyingNode() {
		return underlyingNode;
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Library)) {
			return false;
		}
		return ((Library) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "Library[" + getName() + "]";
	}

}
