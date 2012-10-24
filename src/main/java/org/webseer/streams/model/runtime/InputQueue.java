package org.webseer.streams.model.runtime;

import name.levering.ryan.util.IterableUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.streams.model.program.Neo4JProgramUtils;
import org.webseer.streams.model.program.TransformationNodeInput;
import org.webseer.streams.model.trace.InputGroup;
import org.webseer.streams.model.trace.ItemView;

/**
 * Input is the runtime equivalent of the edge between an output and an input. It mainly keeps track of the current item
 * queue for transformation. It starts off connected to just the target runtime input and is then hooked into the bucket
 * that it draws from.
 */
public class InputQueue {

	private final Node underlyingNode;

	public InputQueue(GraphDatabaseService service, TransformationNodeInput input, RuntimeTransformationNode node) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(Neo4JProgramUtils.getNode(input), NeoRelationshipType.QUEUE_TARGET);
		this.underlyingNode.createRelationshipTo(node.getUnderlyingNode(), NeoRelationshipType.QUEUE_RUNTIME);
	}

	public InputQueue(Node startNode) {
		this.underlyingNode = startNode;
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public TransformationNodeInput getInput() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.QUEUE_TARGET, TransformationNodeInput.class);
	}

	public ItemView getFirstItem() {
		return Neo4JUtils.getLinked(this.underlyingNode, NeoRelationshipType.QUEUE_FIRST, ItemView.class);
	}

	public ItemView getLastItem() {
		return Neo4JUtils.getLinked(this.underlyingNode, NeoRelationshipType.QUEUE_LAST, ItemView.class);
	}

	public void addItem(ItemView item) {
		ItemView last = getLastItem();
		if (last == null) {
			this.underlyingNode.createRelationshipTo(item.getUnderlyingNode(), NeoRelationshipType.QUEUE_FIRST);
		} else {
			last.getUnderlyingNode().createRelationshipTo(item.getUnderlyingNode(), NeoRelationshipType.QUEUE_NEXT);
			this.underlyingNode.getSingleRelationship(NeoRelationshipType.QUEUE_LAST, Direction.OUTGOING).delete();
		}
		this.underlyingNode.createRelationshipTo(item.getUnderlyingNode(), NeoRelationshipType.QUEUE_LAST);
	}

	public String toString() {
		return "InputQueue[" + this.getInput().getInputField().getName() + "]";
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof InputQueue)) {
			return false;
		}
		return ((InputQueue) o).getUnderlyingNode().equals(underlyingNode);
	}

	public static ItemView getNext(ItemView current) {
		return Neo4JUtils.getOutgoing(current.getUnderlyingNode(), NeoRelationshipType.QUEUE_NEXT, ItemView.class);
	}

	public void checkForDelete() {
		if (IterableUtils.size(getInputGroups()) == 0) {
			delete();
		}
	}

	public void delete() {
		System.out.println("Deleting input queue");
		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}
		underlyingNode.delete();
	}

	public Iterable<InputGroup> getInputGroups() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.INPUTGROUP_INPUTQUEUE, InputGroup.class);
	}

}
