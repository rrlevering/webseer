package org.webseer.model.meta;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * A field just has a name and a type. It's a useful abstraction for both inputpoints/outputpoints and their pieces.
 * 
 * @author ryan
 */
public class TransformationField {

	protected final Node underlyingNode;

	public TransformationField(NeoService service, TransformationField parentField, Field field) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		if (parentField != null) {
			this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(parentField),
					NeoRelationshipType.TRANSFORMATIONFIELD_PARENT);
		}
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(field),
				NeoRelationshipType.TRANSFORMATIONFIELD_FIELD);
	}

	public TransformationField(Node node) {
		this.underlyingNode = node;
	}

	public String getName() {
		if (getParent() != null) {
			return getParent().getName() + "." + getField().getName();
		}
		return getField().getName();
	}

	public TransformationField getParent() {
		return Neo4JUtils.getOutgoing(underlyingNode, NeoRelationshipType.TRANSFORMATIONFIELD_PARENT,
				TransformationField.class);
	}

	public Field getField() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATIONFIELD_FIELD, Field.class);
	}

	public Type getType() {
		return getField().getType();
	}

	public InputPoint asInputPoint() {
		if (underlyingNode.hasRelationship(NeoRelationshipType.TRANSFORMATION_INPUTPOINT, Direction.INCOMING)) {
			return new InputPoint(underlyingNode);
		}
		return null;
	}

	public OutputPoint asOutputPoint() {
		if (underlyingNode.hasRelationship(NeoRelationshipType.TRANSFORMATION_OUTPUTPOINT, Direction.INCOMING)) {
			return new OutputPoint(underlyingNode);
		}
		return null;
	}

	public Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public boolean isVarArgs() {
		return false;
	}

	public InputType getInputType() {
		return InputType.SERIAL;
	}

	public Iterable<TransformationField> getChildren() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATIONFIELD_PARENT,
				Direction.INCOMING, TransformationField.class);
	}

}
