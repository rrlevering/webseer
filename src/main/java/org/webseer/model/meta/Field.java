package org.webseer.model.meta;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * A field just has a name and a type. It's a useful abstraction for both inputpoints/outputpoints and their pieces.
 * 
 * @author ryan
 */
public class Field {

	private final static String NAME = "name";

	protected final Node underlyingNode;

	public Field(NeoService service, Type type, String name, boolean repeated) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.setProperty(NAME, name);
		this.underlyingNode.createRelationshipTo(type.getUnderlyingNode(), NeoRelationshipType.FIELD_TYPE);
		setRepeated(repeated);
	}

	public Field(Node node) {
		this.underlyingNode = node;
	}

	public String getName() {
		return (String) underlyingNode.getProperty(NAME);
	}

	/**
	 * Whether or not this field is multiple (an array).
	 * 
	 * @return whether or not there are multiple items in this field
	 */
	public boolean isRepeated() {
		return underlyingNode.hasProperty("REPEATED");
	}

	public void setRepeated(boolean repeated) {
		if (repeated) {
			underlyingNode.setProperty("REPEATED", true);
		} else {
			underlyingNode.removeProperty("REPEATED");
		}
	}

	public Type getType() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.FIELD_TYPE, Type.class);
	}

	public Node getUnderlyingNode() {
		return this.underlyingNode;
	}

}
