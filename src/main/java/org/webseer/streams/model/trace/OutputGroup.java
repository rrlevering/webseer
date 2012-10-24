package org.webseer.streams.model.trace;

import name.levering.ryan.util.IterableUtils;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

public class OutputGroup {

	private final Node underlyingNode;

	// Create two reference counters: 1) number of items held by workspace buckets and 2) number of input groups
	// touching this output group
	// When the two numbers are zero, we can reclaim

	public OutputGroup(GraphDatabaseService service, Bucket bucket, TransformationGroup group) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(bucket.getUnderlyingNode(), NeoRelationshipType.OUTPUTGROUP_BUCKET);
		System.out.println("Output groups = " + IterableUtils.size(bucket.getOutputGroups()));
		this.underlyingNode.createRelationshipTo(group.getUnderlyingNode(),
				NeoRelationshipType.OUTPUTGROUP_TRANSFORMATIONGROUP);
	}

	public OutputGroup(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public long getOutputGroupId() {
		return this.underlyingNode.getId();
	}

	public Bucket getBucket() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.OUTPUTGROUP_BUCKET, Bucket.class);
	}

	public TransformationGroup getTransformationGroup() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.OUTPUTGROUP_TRANSFORMATIONGROUP,
				TransformationGroup.class);
	}

	public Iterable<Item> getItems() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.ITEM_OUTPUTGROUP,
				new Neo4JUtils.NodeReader<Item>() {

					@Override
					public Item convertNode(Node node) {
						return Neo4JUtils.wrapItem(node);
					}
				});
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public void delete() {
		System.out.println("Deleting output group");

		Bucket bucket = getBucket();
		TransformationGroup tGroup = getTransformationGroup();

		// If this is the last output group in the transformation group, delete the transformation group
		// TransformationGroup tGroup = getTransformationGroup();
		// Now remove the edge
		underlyingNode.getSingleRelationship(NeoRelationshipType.OUTPUTGROUP_TRANSFORMATIONGROUP, Direction.OUTGOING)
				.delete();
		// Iterator<OutputGroup> tIt = tGroup.getOutputGroups().iterator();
		// if (!tIt.hasNext()) {
		// tGroup.delete();
		// }
		underlyingNode.getSingleRelationship(NeoRelationshipType.OUTPUTGROUP_BUCKET, Direction.OUTGOING).delete();

		// Pull out all the items in this output group from the bucket
		for (Item item : getItems()) {
			item.delete();
		}

		// Check whether we can clean up the bucket object
		bucket.checkForDelete();

		// Now trickle this back to check whether the transformation group can be cleaned up
		tGroup.checkForDelete();

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
		if (!(o instanceof OutputGroup)) {
			return false;
		}
		return ((OutputGroup) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "OutputGroup " + underlyingNode.getId();
	}

	public void checkForDelete() {
		// We can delete if all the items in this output group are not referenced as part of another input group
		// For now do this naively
		boolean clearForDelete = true;

		for (Item item : getItems()) {
			if (item.getUnderlyingNode().hasRelationship(NeoRelationshipType.VIEW_DATA, NeoRelationshipType.VIEW_SCOPE)) {
				// Still have references
				clearForDelete = false;
				break;
			}
		}

		if (clearForDelete) {
			delete();
		}
	}

}
