package org.webseer.model.program;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;

/**
 * This interface is the in memory representation of a transformation node, linked to be easily used in programs.
 * 
 * @author Ryan Levering
 */
public class TransformationNode {

	protected final Node underlyingNode;

	public TransformationNode(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
		assert underlyingNode.hasRelationship(NeoRelationshipType.NODE_TRANSFORMATION);
	}

	public TransformationNode(GraphDatabaseService service, TransformationNode node) {
		this.underlyingNode = Neo4JUtils.createNode(service);

		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(node.getTransformation()),
				NeoRelationshipType.NODE_TRANSFORMATION);

		for (TransformationNodeInput input : node.getInputs()) {
			new TransformationNodeInput(service, this, input);
		}
		for (TransformationNodeOutput output : node.getOutputs()) {
			new TransformationNodeOutput(service, this, output);
		}
	}

	public TransformationNode(GraphDatabaseService service, Transformation transformation, TransformationGraph graph) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		if (transformation != null) {
			this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(transformation),
					NeoRelationshipType.NODE_TRANSFORMATION);
			// We only want to generate inputs and outputs for root things
			for (InputPoint input : transformation.getInputPoints()) {
				new TransformationNodeInput(service, this, input);
			}
			for (OutputPoint output : transformation.getOutputPoints()) {
				new TransformationNodeOutput(service, this, output);
			}
		}
		if (graph != null) {
			graph.getUnderlyingNode().createRelationshipTo(underlyingNode, NeoRelationshipType.GRAPH_NODE);
		}
	}

	public WorkspaceBucketNode asWorkspaceBucketNode() {
		if (underlyingNode.hasRelationship(NeoRelationshipType.WORKSPACEBUCKETNODE_WORKSPACEBUCKET)) {
			return new WorkspaceBucketNode(underlyingNode);
		}
		return null;
	}

	public DisconnectedWorkspaceBucketNode asDisconnectedWorkspaceBucketNode() {
		if (underlyingNode.hasProperty(DisconnectedWorkspaceBucketNode.BUCKET_NAME)) {
			return new DisconnectedWorkspaceBucketNode(underlyingNode);
		}
		return null;
	}

	public Transformation getTransformation() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODE_TRANSFORMATION, Transformation.class);
	}

	public TransformationGraph getTransformationGraph() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.GRAPH_NODE, TransformationGraph.class);
	}

	public Iterable<TransformationNodeInput> getInputs() {
		return Neo4JUtils
				.getIterable(underlyingNode, NeoRelationshipType.NODE_NODEINPUT, TransformationNodeInput.class);
	}

	public Iterable<TransformationNodeOutput> getOutputs() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.NODE_NODEOUTPUT,
				TransformationNodeOutput.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public TransformationNodeOutput getOutput(String string) {
		for (TransformationNodeOutput output : getOutputs()) {
			if (output.getOutputField().getName().equals(string)) {
				return output;
			}
		}
		return null;
	}

	public TransformationNodeInput getInput(String string) {
		for (TransformationNodeInput input : getInputs()) {
			if (input.getInputField().getName().equals(string)) {
				return input;
			}
		}
		return null;
	}

	public long getNodeId() {
		return underlyingNode.getId();
	}

	public void delete() {
		System.out.println("Deleting transformation node");

		// FIXME This leaves dangling transformations, but someday they might be shared
		for (TransformationNodeOutput output : getOutputs()) {
			output.delete();
		}
		for (TransformationNodeInput input : getInputs()) {
			input.delete();
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
		if (!(o instanceof TransformationNode)) {
			return false;
		}
		return ((TransformationNode) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return this.getTransformation().getName() + "/" + getNodeId();
	}

	public void setUIProperty(String string, String string2) {
		this.underlyingNode.setProperty("UI:" + string, string2);
	}

	public String getUIProperty(String string) {
		return Neo4JUtils.getString(this.underlyingNode, "UI:" + string);
	}
}
