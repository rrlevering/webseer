package org.webseer.streams.model.trace;

import java.util.Iterator;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.streams.model.runtime.InputQueue;
import org.webseer.streams.model.runtime.Neo4JRuntimeUtils;

/**
 * An input group is everything that is pulled in a single execution of a transformation. So if a transformation is just
 * reading a single value, it will be an input group of one item. If it's pulling in the whole stream, it will be an
 * input group of all the incoming data.
 * 
 * @author ryan
 */
public class InputGroup {

	private final Node underlyingNode;

	/**
	 * @param queue the input queue (runtime structure) that generated this input group. This can be used to track back
	 *            to the input point this
	 */
	public InputGroup(GraphDatabaseService service, InputQueue queue, TransformationGroup group) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(group.getUnderlyingNode(), NeoRelationshipType.SCOPE_GROUP);
		this.underlyingNode.createRelationshipTo(Neo4JRuntimeUtils.getNode(queue),
				NeoRelationshipType.INPUTGROUP_INPUTQUEUE);
	}

	public InputGroup(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public long getInputGroupId() {
		return underlyingNode.getId();
	}

	public InputQueue getInputQueue() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.INPUTGROUP_INPUTQUEUE, InputQueue.class);
	}

	public TransformationGroup getTransformationGroup() {
		return new TransformationGroup(underlyingNode.getSingleRelationship(NeoRelationshipType.SCOPE_GROUP,
				Direction.OUTGOING).getOtherNode(underlyingNode));
	}

	public void advance() {
		if (!underlyingNode.hasRelationship(NeoRelationshipType.INPUTGROUP_FIRST_ITEM)) {
			this.underlyingNode.createRelationshipTo(getInputQueue().getLastItem().getUnderlyingNode(),
					NeoRelationshipType.INPUTGROUP_FIRST_ITEM);
		}
		if (underlyingNode.hasRelationship(NeoRelationshipType.INPUTGROUP_LAST_ITEM)) {
			this.underlyingNode.getSingleRelationship(NeoRelationshipType.INPUTGROUP_LAST_ITEM, Direction.OUTGOING)
					.delete();
		}
		this.underlyingNode.createRelationshipTo(getInputQueue().getLastItem().getUnderlyingNode(),
				NeoRelationshipType.INPUTGROUP_LAST_ITEM);
	}

	public ItemView getFirst() {
		return Neo4JUtils.getLinked(this.underlyingNode, NeoRelationshipType.INPUTGROUP_FIRST_ITEM, ItemView.class);
	}

	public ItemView getLast() {
		return Neo4JUtils.getLinked(this.underlyingNode, NeoRelationshipType.INPUTGROUP_LAST_ITEM, ItemView.class);
	}

	public Iterable<ItemView> getUnsortedItems() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.VIEW_GROUP, ItemView.class);
	}

	public Iterable<ItemView> getItems() {
		return new Iterable<ItemView>() {

			public Iterator<ItemView> iterator() {
				return new BucketItemIterator();
			}

		};
	}

	private class BucketItemIterator implements Iterator<ItemView> {

		private ItemView current = null;

		public boolean hasNext() {
			return (current == null && getFirst() != null)
					|| (InputQueue.getNext(current) != null && !current.equals(getLast()));
		}

		public ItemView next() {
			if (current == null) {
				current = getFirst();
			} else if (current.equals(getLast())) {
				return null;
			} else {
				current = InputQueue.getNext(current);
			}
			return current;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public void delete() {
		System.out.println("Deleting input group");
		underlyingNode.getSingleRelationship(NeoRelationshipType.SCOPE_GROUP, Direction.OUTGOING).delete();

		Neo4JUtils.removeFromInlineList(getFirst().getUnderlyingNode(), getLast().getUnderlyingNode(),
				NeoRelationshipType.QUEUE_FIRST, NeoRelationshipType.QUEUE_LAST, NeoRelationshipType.QUEUE_NEXT);

		if (underlyingNode.hasRelationship(NeoRelationshipType.INPUTGROUP_FIRST_ITEM)) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.INPUTGROUP_FIRST_ITEM, Direction.OUTGOING)
					.delete();
		}
		if (underlyingNode.hasRelationship(NeoRelationshipType.INPUTGROUP_LAST_ITEM)) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.INPUTGROUP_LAST_ITEM, Direction.OUTGOING).delete();
		}

		for (ItemView view : getUnsortedItems()) {
			view.delete();
		}

		InputQueue queue = getInputQueue();
		underlyingNode.getSingleRelationship(NeoRelationshipType.INPUTGROUP_INPUTQUEUE, Direction.OUTGOING).delete();
		queue.checkForDelete();

		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}
		underlyingNode.delete();

	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof InputGroup)) {
			return false;
		}
		return ((InputGroup) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "InputGroup " + underlyingNode.getId();
	}

}
