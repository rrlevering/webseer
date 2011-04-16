package org.webseer.model.trace;

import org.neo4j.api.core.Node;

public class Neo4JTraceUtils {

	public final static Node getNode(Bucket bucket) {
		return bucket.getUnderlyingNode();
	}

	public final static Node getNode(TransformationGroup tGroup) {
		return tGroup.getUnderlyingNode();
	}

}
