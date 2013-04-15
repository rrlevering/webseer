package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * In Java, this represents a class file inside a jar.
 */
public class LibraryResource {

	private final static String NAME = "name";

	private final static String DATA = "data";

	private final Node underlyingNode;

	public LibraryResource(GraphDatabaseService service, Library library, String name, byte[] data) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(library.getUnderlyingNode(),
				NeoRelationshipType.LIBRARY_RESOURCE);
		Neo4JUtils.setProperty(underlyingNode, NAME, name);
		Neo4JUtils.setProperty(underlyingNode, DATA, data);
	}

	public LibraryResource(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}
	
	public String getName() {
		return Neo4JUtils.getString(underlyingNode, NAME);
	}

	public byte[] getData() {
		return Neo4JUtils.getByteArray(underlyingNode, DATA);
	}

	public Library getLibrary() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.LIBRARY_RESOURCE, Library.class);
	}
	
	Node getUnderlyingNode() {
		return underlyingNode;
	}

}
