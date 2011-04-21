package org.webseer.model.program;

import org.neo4j.graphdb.Node;

public class Neo4JProgramUtils {

	public final static Node getNode(TransformationNode transformationNode) {
		return transformationNode.getUnderlyingNode();
	}

	public final static Node getNode(TransformationNodeInput transformationInput) {
		return transformationInput.getUnderlyingNode();
	}

	public final static Node getNode(TransformationNodeOutput transformationOutput) {
		return transformationOutput.getUnderlyingNode();
	}

	public final static Node getNode(TransformationGraph graph) {
		return graph.getUnderlyingNode();
	}

	public static Node getNode(TransformationEdge toClone) {
		return toClone.getUnderlyingNode();
	}

}
