package org.webseer.streams.model.trace;

import java.io.InputStream;
import java.io.OutputStream;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.UserType;

public class DataItem implements Item, HasValue {

	private final Node underlyingNode;

	public DataItem(GraphDatabaseService service, OutputGroup outputGroup) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(outputGroup.getUnderlyingNode(), NeoRelationshipType.ITEM_OUTPUTGROUP);
		outputGroup.getBucket().addItem(this);
	}

	protected DataItem(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public long getItemId() {
		return underlyingNode.getId();
	}

	public OutputGroup getOutputGroup() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.ITEM_OUTPUTGROUP, OutputGroup.class);
	}

	public UserType getType() {
		return getOutputGroup().getBucket().getTransformationNodeOutput().getOutputField().getType();
	}

	public Node getUnderlyingNode() {
		return underlyingNode;
	}

	public static DataItem wrap(Node nodeById) {
		return new DataItem(nodeById);
	}

	public void delete() {
		System.out.println("Deleting item");
		this.underlyingNode.getSingleRelationship(NeoRelationshipType.ITEM_OUTPUTGROUP, Direction.OUTGOING).delete();

		Relationship ref = this.underlyingNode.getSingleRelationship(NeoRelationshipType.ITEM_REFERENCE,
				Direction.OUTGOING);
		if (ref != null) {
			ref.delete();
		}

		// Remove it from the bucket
		Neo4JUtils.removeFromInlineList(underlyingNode, NeoRelationshipType.BUCKET_FIRST,
				NeoRelationshipType.BUCKET_LAST, NeoRelationshipType.BUCKET_NEXT);

		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}
		underlyingNode.delete();
	}

	public String getLabel() {
		return getOutputGroup().getBucket().getTransformationNodeOutput().getOutputField().getName();
	}

	public String toString() {
		return "Item " + getItemId();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof DataItem)) {
			return false;
		}
		return ((DataItem) o).getUnderlyingNode().equals(underlyingNode);
	}

	public Object get() {
		return getOutputGroup().getBucket().getType().getValue(this);
	}

	public Object get(String field) {
		return getOutputGroup().getBucket().getType().getValue(this, field);
	}

	@Override
	public UserType getType(String field) {
		return getOutputGroup().getBucket().getType().getFieldType(field);
	}

	@Override
	public void setValue(Object value) {
		underlyingNode.setProperty(VALUE, value);
	}

	@Override
	public Object getValue() {
		return underlyingNode.getProperty(VALUE, null);
	}

	@Override
	public Long getOffset() {
		return (Long) underlyingNode.getProperty(BUCKET_OFFSET, null);
	}

	@Override
	public long getBucketId() {
		return getOutputGroup().getBucket().getBucketId();
	}

	@Override
	public void setOffset(long first) {
		underlyingNode.setProperty(BUCKET_OFFSET, first);
	}

	@Override
	public InputStream getInputStream() {
		return getType().getInputStream(this);
	}

	public OutputStream getOutputStream() {
		return getType().getOutputStream(this);
	}

}
