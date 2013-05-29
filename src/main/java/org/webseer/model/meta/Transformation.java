package org.webseer.model.meta;

import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.transformation.PullRuntimeTransformation;

/**
 * This is the model class that represents a webseer "macro-function". This transformation may or may not be a
 * valid/found transformation in the current system. It could potentially represent a function that can't be run
 * anymore. Transformations have URIs which are unique for the precise function that is being run. A transformation in
 * theory should be language-agnostic. Transformations are immutable in their implementation and always runnable, which
 * is one of the main points of webseer, allowing a guarantee of a fixed reference point between implementor and
 * consumer.
 */
public abstract class Transformation {

	private static final String NAME = "name";

	private static final String DESCRIPTION = "description";

	private static final String OWNER = "owner";

	private static final String KEYWORDS = "keywords";

	private static final String VERSION = "version";

	protected final Node underlyingNode;

	protected Transformation(Node node, String name) {
		this.underlyingNode = node;
		this.underlyingNode.setProperty(NAME, name);
		this.underlyingNode.setProperty(VERSION, System.currentTimeMillis());
	}

	public Transformation(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public abstract PullRuntimeTransformation getPullRuntimeTransformation() throws TransformationException;

	public abstract String getType();

	public String getName() {
		return Neo4JUtils.getString(underlyingNode, NAME);
	}
	
	public String getSimpleName() {
		String name = getName();
		int lastSegment = name.lastIndexOf('.');
		if (lastSegment < 0) {
			return name;
		}
		return name.substring(lastSegment + 1);
	}

	public void setKeyWords(String[] keyWords) {
		Neo4JUtils.setStringArray(underlyingNode, KEYWORDS, keyWords);
	}

	public String[] getKeyWords() {
		return Neo4JUtils.getStringArray(underlyingNode, KEYWORDS);
	}
	
	public String getOwner() {
		return Neo4JUtils.getString(underlyingNode, OWNER);
	}

	public void setOwner(String owner) {
		underlyingNode.setProperty(OWNER, owner);
	}

	public void setDescription(String description) {
		underlyingNode.setProperty(DESCRIPTION, description);
	}

	public String getDescription() {
		return Neo4JUtils.getString(underlyingNode, DESCRIPTION);
	}

	public long getVersion() {
		return Neo4JUtils.getLong(underlyingNode, VERSION);
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

	public String getPackage() {
		String fullName = getName();
		if (fullName.indexOf('.') < 0) {
			return "";
		}
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
		return "Transformation[" + getName() + " (" + getVersion() + ")]";
	}

	public Transformation getLastTransformation() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATION_LAST_VERSION,
				Transformation.class);
	}

}
