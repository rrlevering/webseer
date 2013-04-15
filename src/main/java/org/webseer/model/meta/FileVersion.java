package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;

public class FileVersion {

	private static final String CODE = "code";

	private final Node underlyingNode;

	public FileVersion(GraphDatabaseService service, String code) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.setProperty(CODE, code);
	}

	public FileVersion(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public String getCode() {
		return Neo4JUtils.getString(underlyingNode, CODE);
	}

	Node getUnderlyingNode() {
		return underlyingNode;
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof FileVersion)) {
			return false;
		}
		return ((FileVersion) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "SourceFile";
	}

}
