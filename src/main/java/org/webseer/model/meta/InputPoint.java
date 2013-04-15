package org.webseer.model.meta;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * An input point of a transformation. This would be like a parameter in a function call.
 * 
 * @author ryan
 */
public class InputPoint extends TransformationField {

	private final static String INPUT_TYPE = "inputType";

	private static final String REQUIRED = "REQUIRED";

	private static final String VARARGS = "VARARGS";

	public InputPoint(GraphDatabaseService service, Transformation transformation, Field field) {
		this(service, transformation, field, InputType.SERIAL, false, false);
	}

	public InputPoint(GraphDatabaseService service, Transformation transformation, Field field,
			InputType inputType, boolean required, boolean varargs) {
		super(service, field);
		this.underlyingNode.createRelationshipTo(transformation.getUnderlyingNode(),
				NeoRelationshipType.TRANSFORMATION_INPUTPOINT);
		this.underlyingNode.setProperty(INPUT_TYPE, inputType.toString());
		this.underlyingNode.setProperty(REQUIRED, required);
		this.underlyingNode.setProperty(VARARGS, varargs);
	}

	public InputPoint(Node underlyingNode) {
		super(underlyingNode);
	}

	public InputType getInputType() {
		return InputType.valueOf((String) underlyingNode.getProperty(INPUT_TYPE));
	}

	public boolean isRequired() {
		return !underlyingNode.hasProperty(REQUIRED) || (Boolean) underlyingNode.getProperty(REQUIRED);
	}

	public boolean isVarArgs() {
		return !underlyingNode.hasProperty(VARARGS) || (Boolean) underlyingNode.getProperty(VARARGS);
	}

	public Transformation getTransformation() {
		return Neo4JUtils.getInstance(underlyingNode.getSingleRelationship(NeoRelationshipType.TRANSFORMATION_INPUTPOINT,
				Direction.INCOMING).getOtherNode(underlyingNode), Transformation.class);
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof InputPoint)) {
			return false;
		}
		return ((InputPoint) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "InputPoint[" + getName() + "]";
	}

	public long getInputPointId() {
		return underlyingNode.getId();
	}

}
