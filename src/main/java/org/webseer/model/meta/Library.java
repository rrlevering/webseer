package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;

/**
 * Represents an abstraction of a library, a set of generally related code that
 * is versioned together.
 * 
 * @author Ryan Levering
 */
public class Library {

	private static final String VERSION = "version";

	private final static String NAME = "name";

	private final static String GROUP = "group";

	private final static String DATA = "data";

	private final Node underlyingNode;

	public Library(GraphDatabaseService service, String group,
			String libraryName, String version, byte[] data) {
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

	Node getUnderlyingNode() {
		return underlyingNode;
	}

}
