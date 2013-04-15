package org.webseer.java;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.java.JavaRuntimeFactory.CompilationFailedException;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.FileVersion;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.PullRuntimeTransformation;

/**
 * Extends a standard transformation to provide access to edges and properties specific to java transformations.
 */
public class JavaTransformation extends Transformation {

	public JavaTransformation(GraphDatabaseService service, String name, FileVersion source) {
		super(Neo4JUtils.createNode(service, JavaTransformation.class), name);
		underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(source), NeoRelationshipType.TRANSFORMATION_SOURCE);
	}

	public JavaTransformation(Node underlyingNode) {
		super(underlyingNode);
	}

	public String getRuntime() {
		return "JRE from " + System.getProperty("java.vendor") + " v" + System.getProperty("java.version");
	}

	public FileVersion getSource() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATION_SOURCE, FileVersion.class);
	}

	public Iterable<Library> getLibraries() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATION_LIBRARY, Library.class);
	}

	@Override
	public PullRuntimeTransformation getPullRuntimeTransformation() throws TransformationException {

		// Write out the Java source
		String className = getName();
		String code = getSource().getCode();

		Object object;
		try {
			Class<?> clazz = JavaRuntimeFactory.getDefaultInstance().getClass(className, code, getLibraries());
			object = clazz.newInstance();
		} catch (InstantiationException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (IllegalAccessException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (CompilationFailedException e) {
			throw new TransformationException("Transformation is not compilable, how did it get created?", e);
		}

		// Load and wrap the object with a java transformation wrapper
		return new ClassTransformation(object);

	}

	@Override
	public String getType() {
		return "Java";
	}

}
