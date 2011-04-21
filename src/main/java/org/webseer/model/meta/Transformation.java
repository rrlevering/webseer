package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * This is the model class that represents a webseer "macro-function". This transformation may or may not be a
 * valid/found transformation in the current system. It could potentially represent a function that can't be run
 * anymore. Transformations have URIs which are unique for the precise function that is being run.
 * 
 * @author ryan
 */
public class Transformation {

	private static final String NAME = "name";

	private static final String DESCRIPTION = "description";

	private static final String CODE = "code";

	private static final String VERSION = "version";

	private final Node underlyingNode;

	public Transformation(GraphDatabaseService service, String name) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.setProperty(NAME, name);
	}

	public Transformation(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public String getName() {
		return Neo4JUtils.getString(underlyingNode, NAME);
	}

	public void setDescription(String description) {
		underlyingNode.setProperty(DESCRIPTION, description);
	}

	public String getDescription() {
		return Neo4JUtils.getString(underlyingNode, DESCRIPTION);
	}

	public String getCode() {
		return Neo4JUtils.getString(underlyingNode, CODE);
	}

	public void setCode(String string) {
		underlyingNode.setProperty(CODE, string);
	}

	public Long getVersion() {
		return Neo4JUtils.getLong(underlyingNode, VERSION);
	}

	public String getLanguage() {
		return "Java";
	}

	public Iterable<InputPoint> getInputPoints() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATION_INPUTPOINT, InputPoint.class);
	}

	public Iterable<OutputPoint> getOutputPoints() {
		return Neo4JUtils
				.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATION_OUTPUTPOINT, OutputPoint.class);
	}

	public OutputPoint getOutput(String name) {
		for (OutputPoint output : getOutputPoints()) {
			if (output.getName().equals(name)) {
				return output;
			}
		}
		return null;
	}

	public String getSimpleName() {
		String fullName = getName();
		return fullName.substring(fullName.lastIndexOf('.') + 1);
	}

	public String getPackage() {
		String fullName = getName();
		return fullName.substring(0, fullName.lastIndexOf('.'));
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Transformation)) {
			return false;
		}
		return ((Transformation) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "Transformation[" + getName() + "]";
	}

	public void setVersion(long version) {
		underlyingNode.setProperty(VERSION, version);
	}

}
