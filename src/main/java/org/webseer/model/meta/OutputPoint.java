package org.webseer.model.meta;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.webseer.model.NeoRelationshipType;

/**
 * An output point of a transformation is similar to a return value of a function. In webseer, transformations can
 * produce multiple outputs, potentially at multiple times. Multiple things may be output from the same output point
 * within the same function run. webseer handles collections as streams of individual items with only order.
 * 
 * @author ryan
 */
public class OutputPoint extends TransformationField {

	public OutputPoint(NeoService service, Transformation transformation, String name, Type type) {
		super(service, null, new Field(service, type, name, false));
		this.underlyingNode.createRelationshipTo(transformation.getUnderlyingNode(),
				NeoRelationshipType.TRANSFORMATION_OUTPUTPOINT);
	}

	public OutputPoint(Node underlyingNode) {
		super(underlyingNode);
	}

	public Transformation getTransformation() {
		return new Transformation(underlyingNode.getSingleRelationship(NeoRelationshipType.TRANSFORMATION_OUTPUTPOINT,
				Direction.INCOMING).getOtherNode(underlyingNode));
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof OutputPoint)) {
			return false;
		}
		return ((OutputPoint) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "OutputPoint[" + getName() + "]";
	}

	public long getOutputPointId() {
		return underlyingNode.getId();
	}

}
