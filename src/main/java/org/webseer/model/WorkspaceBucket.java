package org.webseer.model;

import java.util.List;

import name.levering.ryan.util.IterableUtils;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils.NodeReader;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Type;
import org.webseer.model.program.WorkspaceBucketNode;
import org.webseer.model.trace.InputGroup;
import org.webseer.model.trace.ItemView;
import org.webseer.model.trace.Neo4JTraceUtils;
import org.webseer.model.trace.TransformationGroup;

/**
 * A workspace bucket is the form of persistence in webseer. Buckets of objects can be persisted with a particular name.
 * This is the only form of permanence, anything not in a workspace bucket is transient and will be destroyed after a
 * program run.
 * 
 * @author ryan
 */
public class WorkspaceBucket {

	private static final String NAME = "name";

	private final Node underlyingNode;

	public WorkspaceBucket(GraphDatabaseService service, Workspace workspace, String name) {
		if (workspace != null) {
			if (workspace.getWorkspaceBucket(name) != null) {
				throw new IllegalArgumentException("Name must be unique to workspace");
			}
		}
		this.underlyingNode = Neo4JUtils.createNode(service);
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Must not be empty");
		}
		underlyingNode.setProperty(NAME, name);
		if (workspace != null) {
			underlyingNode.createRelationshipTo(workspace.getUnderlyingNode(),
					NeoRelationshipType.WORKSPACEBUCKET_WORKSPACE);
		}
	}

	public WorkspaceBucket(GraphDatabaseService service, PreviewBuffer buffer) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		underlyingNode.createRelationshipTo(buffer.getUnderlyingNode(), NeoRelationshipType.WORKSPACEBUCKET_WORKSPACE);
		underlyingNode.setProperty(NAME, "PREVIEW");
	}

	public WorkspaceBucket(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public Workspace getWorkspace() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.WORKSPACEBUCKET_WORKSPACE, Workspace.class);
	}

	public void setName(String name) {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Must not be empty");
		}
		underlyingNode.setProperty(NAME, name);
	}

	public String getName() {
		return (String) underlyingNode.getProperty(NAME);
	}

	public Iterable<ItemView> getItems() {
		return Neo4JUtils.getListIterable(underlyingNode, NeoRelationshipType.WORKSPACEBUCKET_FIRST,
				NeoRelationshipType.WORKSPACEBUCKET_ITEM, new NodeReader<ItemView>() {

					@Override
					public ItemView convertNode(Node node) {
						return Neo4JUtils.getWrapped(node, ItemView.class);
					}

				});
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public void addBucketItem(GraphDatabaseService service, ItemView item) {
		// If this is the first item in the bucket, set the type
		if (!underlyingNode.hasRelationship(NeoRelationshipType.WORKSPACEBUCKET_FIRST, Direction.OUTGOING)) {
			setType(item.getType());
		}
		Neo4JUtils.addToList(service, underlyingNode, item.getUnderlyingNode(),
				NeoRelationshipType.WORKSPACEBUCKET_FIRST, NeoRelationshipType.WORKSPACEBUCKET_LAST,
				NeoRelationshipType.WORKSPACEBUCKET_ITEM);

		increment(item.getInputGroup().getTransformationGroup());
	}

	/**
	 * Removes an item from a bucket, walking back the transformation graph and seeing if we can delete any of the items
	 * in previous buckets. If a bucket is empty, the bucket store can be deleted.
	 * 
	 * @param item the item to remove from the workspace bucket
	 */
	public void removeItem(ItemView item) {
		Neo4JUtils.removeFromList(underlyingNode, item.getUnderlyingNode(), NeoRelationshipType.WORKSPACEBUCKET_FIRST,
				NeoRelationshipType.WORKSPACEBUCKET_LAST, NeoRelationshipType.WORKSPACEBUCKET_ITEM);

		// Check the transformation group to see if it has any items left
		InputGroup group = item.getInputGroup();
		TransformationGroup tGroup = group.getTransformationGroup();
		boolean shouldDestroy = decrement(tGroup);
		if (shouldDestroy) {
			for (Relationship rel : underlyingNode.getRelationships(NeoRelationshipType.WORKSPACE_BUCKET_GROUP)) {
				if (Neo4JTraceUtils.getNode(tGroup).equals(rel.getEndNode())) {
					rel.delete();
				}
			}
			tGroup.delete();
		}

		// If this is the last item in the bucket, clear the type
		if (!underlyingNode.hasRelationship(NeoRelationshipType.WORKSPACEBUCKET_FIRST, Direction.OUTGOING)) {
			setType(null);
		}
	}

	private void increment(TransformationGroup tGroup) {
		for (Relationship rel : underlyingNode.getRelationships(NeoRelationshipType.WORKSPACE_BUCKET_GROUP)) {
			if (Neo4JTraceUtils.getNode(tGroup).equals(rel.getEndNode())) {
				int itemCount = (Integer) rel.getProperty("ITEM_COUNT");
				rel.setProperty("ITEM_COUNT", itemCount + 1);
				return;
			}
		}
		// New tgroup, set property
		Relationship rel = underlyingNode.createRelationshipTo(Neo4JTraceUtils.getNode(tGroup),
				NeoRelationshipType.WORKSPACE_BUCKET_GROUP);
		rel.setProperty("ITEM_COUNT", 1);
	}

	private boolean decrement(TransformationGroup tGroup) {
		for (Relationship rel : underlyingNode.getRelationships(NeoRelationshipType.WORKSPACE_BUCKET_GROUP)) {
			if (Neo4JTraceUtils.getNode(tGroup).equals(rel.getEndNode())) {
				int itemCount = (Integer) rel.getProperty("ITEM_COUNT");
				if (itemCount == 1) {
					rel.removeProperty("ITEM_COUNT");
					return true;
				} else {
					rel.setProperty("ITEM_COUNT", itemCount - 1);
					return false;
				}
			}
		}
		return true;
	}

	public int size() {
		int total = 0;
		for (Relationship rel : underlyingNode.getRelationships(NeoRelationshipType.WORKSPACE_BUCKET_GROUP)) {
			total += (Integer) rel.getProperty("ITEM_COUNT", 0);
		}
		return total;
	}

	public void setType(Type type) {
		if (underlyingNode.hasRelationship(NeoRelationshipType.WORKSPACE_BUCKET_TYPE, Direction.OUTGOING)) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.WORKSPACE_BUCKET_TYPE, Direction.OUTGOING)
					.delete();
		}
		if (type != null) {
			underlyingNode
					.createRelationshipTo(Neo4JMetaUtils.getNode(type), NeoRelationshipType.WORKSPACE_BUCKET_TYPE);
		}
	}

	public Type getType() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.WORKSPACE_BUCKET_TYPE, Type.class);
	}

	public void removeAll() {
		List<ItemView> copy = IterableUtils.toList(getItems());
		for (ItemView view : copy) {
			removeItem(view);
		}
		// Neo4JUtils.clearList(underlyingNode, NeoRelationshipType.WORKSPACEBUCKET_FIRST,
		// NeoRelationshipType.WORKSPACEBUCKET_LAST, NeoRelationshipType.WORKSPACEBUCKET_ITEM,
		// new DeleteHandler() {
		//
		// @Override
		// public void handleDelete(Node node) {
		// Neo4JUtils.getWrapped(node, ItemView.class).getViewScope().getOutputGroup().checkForDelete();
		// }
		//
		// });
	}

	public Iterable<WorkspaceBucketNode> getReferences() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.WORKSPACEBUCKETNODE_WORKSPACEBUCKET,
				WorkspaceBucketNode.class);
	}

	public void delete() {
		removeAll();

		if (underlyingNode.hasRelationship(NeoRelationshipType.WORKSPACE_BUCKET_TYPE, Direction.OUTGOING)) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.WORKSPACE_BUCKET_TYPE, Direction.OUTGOING)
					.delete();
		}

		underlyingNode.getSingleRelationship(NeoRelationshipType.WORKSPACEBUCKET_WORKSPACE, Direction.OUTGOING)
				.delete();

		for (WorkspaceBucketNode node : getReferences()) {
			node.delete();
		}

		underlyingNode.delete();
	}

	public long getBucketId() {
		return underlyingNode.getId();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof WorkspaceBucket)) {
			return false;
		}
		return ((WorkspaceBucket) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "WorkspaceBucket[" + getName() + "]";
	}

	public Iterable<TransformationGroup> getTransformationGroups() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.WORKSPACE_BUCKET_GROUP,
				TransformationGroup.class);
	}
}
