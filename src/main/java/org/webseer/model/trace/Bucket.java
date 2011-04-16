package org.webseer.model.trace;

import java.util.Iterator;

import name.levering.ryan.util.IterableUtils;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Type;
import org.webseer.model.program.Neo4JProgramUtils;
import org.webseer.model.program.TransformationNodeOutput;
import org.webseer.model.runtime.InputQueue;
import org.webseer.model.runtime.Neo4JRuntimeUtils;
import org.webseer.model.runtime.RuntimeTransformationNode;

/**
 * This is a collection of objects. These are used to accumulate the outputs of all the transformations and as inputs to
 * all the proceeding calculations.
 * 
 * @author ryan
 */
public class Bucket {

	private final Node underlyingNode;

	public Bucket(NeoService service, RuntimeTransformationNode runtimeNode, TransformationNodeOutput output) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(Neo4JRuntimeUtils.getNode(runtimeNode),
				NeoRelationshipType.BUCKET_RUNTIME);
		this.underlyingNode.createRelationshipTo(Neo4JProgramUtils.getNode(output),
				NeoRelationshipType.BUCKET_NODEOUTPUT);
	}

	public Bucket(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public long getBucketId() {
		return underlyingNode.getId();
	}

	public Type getType() {
		return getTransformationNodeOutput().getOutputField().getType();
	}

	public Iterable<OutputGroup> getOutputGroups() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.OUTPUTGROUP_BUCKET, OutputGroup.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public void delete() {
		System.out.println("Deleting bucket");

		// Delete any on-disk data
		getType().deleteBucket(this);

		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}

		// underlyingNode.getSingleRelationship(NeoRelationshipType.BUCKET_SOURCE, Direction.OUTGOING).delete();
		underlyingNode.delete();
	}

	public Iterable<InputQueue> getDependentQueues() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.QUEUE_BUCKET, InputQueue.class);
	}

	public TransformationNodeOutput getTransformationNodeOutput() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.BUCKET_NODEOUTPUT,
				TransformationNodeOutput.class);
	}

	public static Item getNext(Item current) {
		Relationship rel = current.getUnderlyingNode().getSingleRelationship(NeoRelationshipType.BUCKET_NEXT,
				Direction.OUTGOING);
		if (rel == null) {
			return null;
		}
		Node nextNode = rel.getEndNode();

		return Neo4JUtils.wrapItem(nextNode);
	}

	public Item getFirstItem() {
		Relationship rel = this.underlyingNode.getSingleRelationship(NeoRelationshipType.BUCKET_FIRST,
				Direction.OUTGOING);
		if (rel == null) {
			return null;
		}
		return Neo4JUtils.wrapItem(rel.getEndNode());
	}

	public Item getLastItem() {
		Relationship rel = this.underlyingNode.getSingleRelationship(NeoRelationshipType.BUCKET_LAST,
				Direction.OUTGOING);
		if (rel == null) {
			return null;
		}
		return Neo4JUtils.wrapItem(rel.getEndNode());
	}

	public void addItem(Item item) {
		Item last = getLastItem();
		if (last == null) {
			this.underlyingNode.createRelationshipTo(item.getUnderlyingNode(), NeoRelationshipType.BUCKET_FIRST);
		} else {
			last.getUnderlyingNode().createRelationshipTo(item.getUnderlyingNode(), NeoRelationshipType.BUCKET_NEXT);
			this.underlyingNode.getSingleRelationship(NeoRelationshipType.BUCKET_LAST, Direction.OUTGOING).delete();
		}
		this.underlyingNode.createRelationshipTo(item.getUnderlyingNode(), NeoRelationshipType.BUCKET_LAST);
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Bucket)) {
			return false;
		}
		return ((Bucket) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "Bucket " + underlyingNode.getId();
	}

	public Iterable<Item> getItems() {
		return new Iterable<Item>() {

			public Iterator<Item> iterator() {
				return new Iterator<Item>() {

					Item last = null;

					public boolean hasNext() {
						if (last == null) {
							return getFirstItem() != null;
						}
						return getNext(last) != null;
					}

					public Item next() {
						if (last == null) {
							last = getFirstItem();
							return last;
						}
						last = getNext(last);
						return last;
					}

					public void remove() {
						throw new UnsupportedOperationException("Remove not supported");
					}

				};
			}

		};
	}

	public void addItem(NeoService service, OutputGroup currentOutputGroup, Object next) {
		DataItem item = new DataItem(service, currentOutputGroup);
		getType().setValue(item, next);
	}

	/**
	 * If none of the items in this bucket are referenced as inputs to other runtime transformations or workspace
	 * buckets, then delete this bucket. This will also delete all the output groups.
	 */
	public void checkForDelete() {
		if (IterableUtils.size(getOutputGroups()) == 0) {
			delete();
		} else {
			System.out.println("Can't delete bucket, still has " + IterableUtils.size(getOutputGroups())
					+ " output groups");
			// for (Item item : getItems()) {
			// System.out.println(item.get());
			// }
		}
	}

	public RuntimeTransformationNode getRuntimeNode() {
		return Neo4JUtils
				.getLinked(underlyingNode, NeoRelationshipType.BUCKET_RUNTIME, RuntimeTransformationNode.class);
	}
}
