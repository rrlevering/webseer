package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

public class SourceFile {

	private static final String NAME = "name";

	private static final String CODE = "code";

	private static final String VERSION = "version";

	private final Node underlyingNode;

	public SourceFile(GraphDatabaseService service, String name, String code, Long version) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.setProperty(NAME, name);
		this.underlyingNode.setProperty(CODE, code);
		this.underlyingNode.setProperty(VERSION, version);
	}
	
	public SourceFile(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public String getCode() {
		return Neo4JUtils.getString(underlyingNode, CODE);
	}

	public String getName() {
		return Neo4JUtils.getString(underlyingNode, NAME);
	}

	public Long getVersion() {
		return Neo4JUtils.getLong(underlyingNode, VERSION);
	}

	public void addTransformation(Transformation transformation) {
		underlyingNode.createRelationshipTo(transformation.getUnderlyingNode(),
				NeoRelationshipType.TRANSFORMATION_SOURCE);
	}

}
