package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;

/**
 * This is the model class that represents a webseer "macro-function". This
 * transformation may or may not be a valid/found transformation in the current
 * system. It could potentially represent a function that can't be run anymore.
 * Transformations have URIs which are unique for the precise function that is
 * being run.
 * 
 * @author ryan
 */
public class Transformation {

	private static final String NAME = "name";

	private static final String DESCRIPTION = "description";

	private static final String KEYWORDS = "keywords";

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

	public void setDescription(String description) {
		underlyingNode.setProperty(DESCRIPTION, description);
	}

	public String getDescription() {
		return Neo4JUtils.getString(underlyingNode, DESCRIPTION);
	}

	public String getLanguage() {
		return "Java";
	}

	public String getRuntime() {
		return "JRE from " + System.getProperty("java.vendor") + " v" + System.getProperty("java.version");
	}

	public Iterable<InputPoint> getInputPoints() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATION_INPUTPOINT, InputPoint.class);
	}

	public Iterable<OutputPoint> getOutputPoints() {
		return Neo4JUtils
				.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATION_OUTPUTPOINT, OutputPoint.class);
	}

	public Iterable<Library> getLibraries() {
		return Neo4JUtils
				.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATION_LIBRARY, Library.class);
	}
	
	public SourceFile getSource() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATION_SOURCE, SourceFile.class);
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

}
