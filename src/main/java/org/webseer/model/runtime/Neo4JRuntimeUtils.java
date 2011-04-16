package org.webseer.model.runtime;

import org.neo4j.api.core.Node;

public class Neo4JRuntimeUtils {

	public final static Node getNode(RuntimeTransformationNode runtimeNode) {
		return runtimeNode.getUnderlyingNode();
	}

	public static Node getNode(InputQueue queue) {
		return queue.getUnderlyingNode();
	}

}
