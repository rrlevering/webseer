package org.webseer.model.program;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.Program;
import org.webseer.model.Workspace;
import org.webseer.model.meta.Type;

public class TransformationGraph {

	private final Node underlyingNode;

	public TransformationGraph(GraphDatabaseService service) {
		this.underlyingNode = Neo4JUtils.createNode(service);
	}

	public TransformationGraph(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public Iterable<TransformationNode> getNodes() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.GRAPH_NODE, TransformationNode.class);
	}

	static TransformationGraph wrap(Node node) {
		return new TransformationGraph(node);
	}

	protected Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public static TransformationNode createRuntimeGraph(GraphDatabaseService service, TransformationNode node,
			Map<Node, Node> converted) {
		if (converted.containsKey(node.getUnderlyingNode())) {
			return new TransformationNode(converted.get(node.getUnderlyingNode()));
		}

		// Create
		TransformationNode newNode;
		if (node.asWorkspaceBucketNode() != null) {
			newNode = new DisconnectedWorkspaceBucketNode(new WorkspaceBucketNode(service, null, node
					.asWorkspaceBucketNode().getLinkedBucket()));
		} else {
			newNode = new TransformationNode(service, node);
		}

		converted.put(node.getUnderlyingNode(), newNode.getUnderlyingNode());

		if (node.asWorkspaceBucketNode() == null) {
			// Now walk back through the connections
			for (TransformationNodeInput input : node.getInputs()) {
				for (TransformationEdge edge : input.getIncomingEdges()) {
					createRuntimeGraph(service, edge, newNode, edge.getInput().getInputField().getName(), converted);
				}
			}
		}

		return newNode;
	}

	public static TransformationEdge createRuntimeGraph(GraphDatabaseService service, TransformationEdge edge,
			TransformationNode target, String targetInput, Map<Node, Node> converted) {
		TransformationNode source = createRuntimeGraph(service, edge.getOutput().getTransformationNode(), converted);
		TransformationEdge newEdge = new TransformationEdge(service, source.getOutput(edge.getOutput().getOutputField()
				.getName()), target.getInput(targetInput));
		newEdge.setInputField(edge.getInputField());
		newEdge.setOutputField(edge.getOutputField());
		System.out.println("Output field:" + edge.getOutputField());

		// If we have a projection point and it's part of the the current graph, we need to get a reference to it's
		// clone
		// Otherwise, we directly reference it
		TransformationNodeInput input = edge.getLinkedPoint();
		if (input != null
				&& input.getTransformationNode().getTransformationGraph() != null
				&& input.getTransformationNode().getTransformationGraph()
						.equals(edge.getInput().getTransformationNode().getTransformationGraph())) {
			// Reference the clone
			TransformationNode linkedNode = createRuntimeGraph(service, input.getTransformationNode(), converted);
			newEdge.setLinkedPoint(linkedNode.getInput(input.getInputField().getName()));
		} else {
			newEdge.setLinkedPoint(input);
		}

		return newEdge;
	}

	/**
	 * A clone just clones the program structure of the graph (the nodes, inputs, and outputs). The meta level is left
	 * alone and there is no runtime or trace level yet.
	 */
	public static <T> T clone(GraphDatabaseService service, Node startNode, Class<T> nodeClass, boolean link) {
		Transaction tran = service.beginTx();

		try {

			T clone = Neo4JUtils.getWrapped(recursiveCopy(service, startNode, Arrays.asList( //
					// NeoRelationshipType.GRAPH_NODE, // Graph to all the transformation nodes
					NeoRelationshipType.NODE_NODEINPUT, // Nodes to all their inputs
					NeoRelationshipType.NODE_NODEOUTPUT, // Nodes to all their outputs
					NeoRelationshipType.NODEINPUT_EDGE, // Node inputs to edges
					NeoRelationshipType.NODEOUTPUT_EDGE, // Edges to node outputs
					NeoRelationshipType.EDGE_SOURCE // Flashback
					), link), nodeClass);
			tran.success();
			return clone;
		} finally {
			tran.finish();
		}
	}

	private static Node recursiveCopy(GraphDatabaseService service, Node nodeToCopy,
			List<? extends RelationshipType> edgesToFollow, boolean link) {
		return recursiveCopy(service, nodeToCopy, edgesToFollow, new HashMap<Node, Node>(), link);
	}

	private static Node recursiveCopy(GraphDatabaseService service, Node nodeToCopy,
			List<? extends RelationshipType> edgesToFollow, Map<Node, Node> oldToNew, boolean link) {
		Node newNode = copyNodeAndProperties(service, nodeToCopy);
		oldToNew.put(nodeToCopy, newNode);
		for (Relationship relationship : nodeToCopy.getRelationships()) {
			if (edgesToFollow.contains(relationship.getType())) {
				if (relationship.getStartNode().equals(nodeToCopy)) {
					Node newEnd = oldToNew.get(relationship.getEndNode());
					if (newEnd == null) {
						newEnd = recursiveCopy(service, relationship.getEndNode(), edgesToFollow, oldToNew, link);
					}
					newNode.createRelationshipTo(newEnd, relationship.getType());
				} else {
					// Recur but don't create any edges
					Node newEnd = oldToNew.get(relationship.getStartNode());
					if (newEnd == null) {
						newEnd = recursiveCopy(service, relationship.getStartNode(), edgesToFollow, oldToNew, link);
					}
				}
			} else if (link) {
				// Automatically copy the relationship to the existing node
				if (relationship.getStartNode().equals(nodeToCopy)) {
					System.out.println("Copying " + relationship.getType() + ":" + relationship.getStartNode() + "->"
							+ relationship.getEndNode() + " to " + newNode + "->" + relationship.getEndNode());
					newNode.createRelationshipTo(relationship.getEndNode(), relationship.getType());
				} else {
					System.out.println("Copying " + relationship.getType() + ":" + relationship.getEndNode() + "->"
							+ relationship.getStartNode() + " to " + relationship.getEndNode() + "->" + newNode);
					relationship.getStartNode().createRelationshipTo(newNode, relationship.getType());
				}
			}
		}
		return newNode;
	}

	private static Node copyNodeAndProperties(GraphDatabaseService service, Node nodeToCopy) {
		Node copyNode = Neo4JUtils.createNode(service);
		for (String property : nodeToCopy.getPropertyKeys()) {
			copyNode.setProperty(property, nodeToCopy.getProperty(property));
		}

		// Copy any buckets
		Type.reader.copyBucketData(nodeToCopy.getId(), copyNode.getId());
		return copyNode;
	}

	public long getGraphId() {
		return underlyingNode.getId();
	}

	public static TransformationGraph get(GraphDatabaseService service, long id) {
		return wrap(service.getNodeById(id));
	}

	public void delete() {
		System.out.println("Deleting graph");
		underlyingNode.delete();
	}

	public Program getProgram() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.PROGRAM_GRAPH, Program.class);
	}

	public Workspace getWorkspace() {
		return getProgram().getWorkspace();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TransformationGraph)) {
			return false;
		}
		return ((TransformationGraph) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "Transformation Graph " + underlyingNode.getId();
	}

}
