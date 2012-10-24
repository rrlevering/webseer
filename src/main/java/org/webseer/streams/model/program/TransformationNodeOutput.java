package org.webseer.streams.model.program;

import java.util.Iterator;

import org.apache.commons.lang.ObjectUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.TransformationField;

public class TransformationNodeOutput {

	private final Node underlyingNode;

	public TransformationNodeOutput(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public TransformationNodeOutput(GraphDatabaseService service, TransformationNode node,
			TransformationField outputPoint) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(node.getUnderlyingNode(), NeoRelationshipType.NODE_NODEOUTPUT);
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(outputPoint),
				NeoRelationshipType.NODEOUTPUT_FIELD);
	}

	public TransformationNodeOutput(GraphDatabaseService service, TransformationNode node,
			TransformationNodeOutput output) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(node.getUnderlyingNode(), NeoRelationshipType.NODE_NODEOUTPUT);
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(output.getOutputField()),
				NeoRelationshipType.NODEOUTPUT_FIELD);
	}

	public OutputPoint getOutputField() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODEOUTPUT_FIELD, OutputPoint.class);
	}

	public TransformationNode getTransformationNode() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODE_NODEOUTPUT, TransformationNode.class);
	}

	public TransformationEdge addOutgoingEdge(GraphDatabaseService service, TransformationNodeInput input) {
		return new TransformationEdge(service, this, input);
	}

	public Iterable<TransformationEdge> getOutgoingEdges() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.NODEOUTPUT_EDGE, TransformationEdge.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public void removeOutgoingEdge(TransformationNodeInput nodeInput, String sourceField, String targetField) {
		for (TransformationEdge edge : Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.NODEOUTPUT_EDGE,
				TransformationEdge.class)) {
			if (edge.getInput().equals(nodeInput) && ObjectUtils.equals(sourceField, edge.getOutputField())
					&& ObjectUtils.equals(targetField, edge.getInputField())) {
				edge.delete();
			}
		}
	}

	public void removeOutgoingEdge(TransformationNodeInput nodeInput) {
		removeOutgoingEdge(nodeInput, null, null);
	}

	public static String toString(Iterable<?> iterable) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<?> it = iterable.iterator(); it.hasNext();) {
			builder.append(it.next().toString());
			if (it.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}

	public void delete() {
		System.out.println("Deleting transformation node output");
		for (TransformationEdge edge : getOutgoingEdges()) {
			edge.delete();
		}

		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}

		underlyingNode.delete();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TransformationNodeOutput)) {
			return false;
		}
		return ((TransformationNodeOutput) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "TransformationNodeOutput[" + getOutputField().getName() + "]";
	}

	public long getId() {
		return underlyingNode.getId();
	}
}
