package org.webseer.model.meta;

import org.neo4j.graphdb.Node;
import org.webseer.bucket.LogBucket;
import org.webseer.model.Neo4JUtils;

public abstract class Bucket implements LogBucket {

	private static final String NAME = "name";
	private static final String OWNER = "owner";

	private final Node underlyingNode;

	public Bucket(Node node, String name) {
		this.underlyingNode = node;
		this.underlyingNode.setProperty(NAME, name);
	}

	public Bucket(Node node) {
		this.underlyingNode = node;
	}

	public String getName() {
		return Neo4JUtils.getString(underlyingNode, NAME);
	}

	public String getOwner() {
		return Neo4JUtils.getString(underlyingNode, OWNER);
	}

	public void setOwner(String owner) {
		this.underlyingNode.setProperty(OWNER, owner);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

}
