package org.webseer.streams.model.program;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

public class TransformationEdge {

	private final Node underlyingNode;

	public TransformationEdge(GraphDatabaseService service, TransformationNodeOutput output,
			TransformationNodeInput input) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		if (!input.getInputField().isVarArgs()) {
			assert input.getIncomingEdge() == null;
		}
		this.underlyingNode.createRelationshipTo(output.getUnderlyingNode(), NeoRelationshipType.NODEOUTPUT_EDGE);
		this.underlyingNode.createRelationshipTo(input.getUnderlyingNode(), NeoRelationshipType.NODEINPUT_EDGE);
	}

	public TransformationEdge(Node node) {
		this.underlyingNode = node;
		assert this.underlyingNode.hasRelationship(NeoRelationshipType.NODEINPUT_EDGE);
		assert this.underlyingNode.hasRelationship(NeoRelationshipType.NODEOUTPUT_EDGE);
	}

	public TransformationNodeInput getInput() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODEINPUT_EDGE, TransformationNodeInput.class);
	}

	public TransformationNodeOutput getOutput() {
		return Neo4JUtils
				.getLinked(underlyingNode, NeoRelationshipType.NODEOUTPUT_EDGE, TransformationNodeOutput.class);
	}

	/**
	 * This is confusing, but one of the magical parts of webseer. This is the actual output this edge draws from. At
	 * runtime, the history of the objects that are the actual output points are pulled from here
	 */
	public void setLinkedPoint(TransformationNodeInput input) {
		// Remove the old link
		if (underlyingNode.hasRelationship(NeoRelationshipType.EDGE_SOURCE)) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.EDGE_SOURCE, Direction.OUTGOING).delete();
		}
		if (input != null) {
			System.out.println("Setting linked point");
			underlyingNode.createRelationshipTo(input.getUnderlyingNode(), NeoRelationshipType.EDGE_SOURCE);
		}
	}

	public TransformationNodeInput getLinkedPoint() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.EDGE_SOURCE, TransformationNodeInput.class);
	}

	public void setOutputField(String field) {
		if (field == null) {
			this.underlyingNode.removeProperty("OUTPUT_FIELD");
		} else {
			this.underlyingNode.setProperty("OUTPUT_FIELD", field);
		}
	}

	public String getOutputField() {
		return Neo4JUtils.getString(underlyingNode, "OUTPUT_FIELD");
	}

	public void setInputField(String field) {
		if (field == null) {
			this.underlyingNode.removeProperty("INPUT_FIELD");
		} else {
			this.underlyingNode.setProperty("INPUT_FIELD", field);
		}
	}

	public String getInputField() {
		return Neo4JUtils.getString(underlyingNode, "INPUT_FIELD");
	}

	public void delete() {
		System.out.println("Deleting transformation edge");
		underlyingNode.getSingleRelationship(NeoRelationshipType.NODEINPUT_EDGE, Direction.OUTGOING).delete();
		underlyingNode.getSingleRelationship(NeoRelationshipType.NODEOUTPUT_EDGE, Direction.OUTGOING).delete();

		if (underlyingNode.hasRelationship(NeoRelationshipType.EDGE_SOURCE)) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.EDGE_SOURCE, Direction.OUTGOING).delete();
		}

		underlyingNode.delete();
	}

	Node getUnderlyingNode() {
		return underlyingNode;
	}

	public String toString() {
		return getOutput() + "->" + getInput();
	}

}
