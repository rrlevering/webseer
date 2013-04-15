package org.webseer.model.meta;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * A field just has a name and a type. It's a useful abstraction for both inputpoints/outputpoints and their pieces.
 * 
 * @author ryan
 */
public class TransformationField {

	protected final Node underlyingNode;

	public TransformationField(GraphDatabaseService service, Field field) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(field),
				NeoRelationshipType.TRANSFORMATIONFIELD_FIELD);
	}

	public TransformationField(Node node) {
		this.underlyingNode = node;
	}

	public String getName() {
		return getField().getName();
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

}
