package org.webseer.model.meta;

import org.neo4j.graphdb.Node;

public class Neo4JMetaUtils {

	public final static Node getNode(Transformation transformation) {
		return transformation.getUnderlyingNode();
	}

	public final static Node getNode(InputPoint input) {
		return input.getUnderlyingNode();
	}

	public final static Node getNode(OutputPoint output) {
		return output.getUnderlyingNode();
	}

	public static Node getNode(Type type) {
		return type.getUnderlyingNode();
	}

	public static Node getNode(Field inputPoint) {
		return inputPoint.getUnderlyingNode();
	}

	public static Node getNode(TransformationField inputPoint) {
		return inputPoint.getUnderlyingNode();
	}

}
