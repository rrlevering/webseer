package org.webseer.streams.model.program;

import java.io.InputStream;
import java.io.OutputStream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Type;
import org.webseer.streams.model.trace.HasValue;
import org.webseer.streams.model.trace.Item;
import org.webseer.streams.model.trace.OutputGroup;

public class TransformationNodeInput implements Item, HasValue {

	private final Node underlyingNode;

	public TransformationNodeInput(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public TransformationNodeInput(GraphDatabaseService service, TransformationNode node, InputPoint inputPoint) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(node.getUnderlyingNode(), NeoRelationshipType.NODE_NODEINPUT);
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(inputPoint),
				NeoRelationshipType.NODEINPUT_FIELD);
	}

	public TransformationNodeInput(GraphDatabaseService service, TransformationNode node, TransformationNodeInput input) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(node.getUnderlyingNode(), NeoRelationshipType.NODE_NODEINPUT);
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(input.getInputField()),
				NeoRelationshipType.NODEINPUT_FIELD);
		setValue(input.getValue());
		setMeta(input.getMeta());
		Type.reader.copyBucketData(input.getId(), underlyingNode.getId());
	}

	public InputPoint getInputField() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODEINPUT_FIELD, InputPoint.class);
	}

	public TransformationNode getTransformationNode() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODE_NODEINPUT, TransformationNode.class);
	}

	public TransformationEdge getIncomingEdge() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODEINPUT_EDGE, TransformationEdge.class);
	}

	public Iterable<TransformationEdge> getIncomingEdges() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.NODEINPUT_EDGE, TransformationEdge.class);
	}

	public void setMeta(String meta) {
		Neo4JUtils.setProperty(this.underlyingNode, "META", meta);
	}

	public String getMeta() {
		return Neo4JUtils.getString(underlyingNode, "META");
	}

	public void setValue(Object value) {
		Neo4JUtils.setProperty(underlyingNode, VALUE, value);
	}

	public Object getValue() {
		return this.underlyingNode.getProperty(VALUE, null);
	}

	public Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TransformationNodeInput)) {
			return false;
		}
		return ((TransformationNodeInput) o).getUnderlyingNode().equals(underlyingNode);
	}

	public void delete() {
		System.out.println("Deleting transformation node input");
		getType().deleteBucket(this);

		for (TransformationEdge edge : getIncomingEdges()) {
			edge.delete();
		}

		// Delete the edge relationships, but not the connected outputs
		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}

		underlyingNode.delete();
	}

	public String toString() {
		return "TransformationNodeInput[" + getInputField().getName() + "]";
	}

	public boolean hasValue() {
		return this.underlyingNode.hasProperty("VALUE");
	}

	@Override
	public Long getOffset() {
		return 0L; // Always read from the beginning
	}

	@Override
	public long getBucketId() {
		return underlyingNode.getId();
	}

	@Override
	public void setOffset(long first) {
		// Ignore
	}

	@Override
	public Type getType() {
		return getInputField().getType();
	}

	@Override
	public Type getType(String field) {
		return getInputField().getType().getFieldType(field);
	}

	@Override
	public Object get() {
		return getType().getValue(this);
	}

	@Override
	public Object get(String field) {
		return getType().getValue(this, field);
	}

	@Override
	public InputStream getInputStream() {
		return getType().getInputStream(this);
	}

	public OutputStream getOutputStream() {
		return getType().getOutputStream(this);
	}

	public long getId() {
		return underlyingNode.getId();
	}

	@Override
	public long getItemId() {
		return this.getId();
	}

	@Override
	public OutputGroup getOutputGroup() {
		return null;
	}

}
