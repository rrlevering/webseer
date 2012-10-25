package org.webseer.transformation;

import java.io.IOException;
import java.io.Reader;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.Type;

public interface RuntimeFactory {

	/**
	 * Generates a meta-class, representing a particular data structure in the underlying language.
	 */
	public Type generateType(GraphDatabaseService service, String typeName, Reader reader, long version)
			throws IOException;

	/**
	 * This is the compile-time abstraction for a given language. It takes a reader and readers for all of the
	 * dependencies of the given transformation and returns a parsed transformation.
	 * 
	 * @throws IOException
	 */
	public Transformation generateTransformation(GraphDatabaseService service, String qualifiedName, Reader reader,
			long version) throws IOException;

	/**
	 * This is the runtime layer abstraction for a given language. It generates a transformation that can actually be
	 * run to pull from inputs and push transformed output channels.
	 */
	public PullRuntimeTransformation generatePullTransformation(Transformation transformation)
			throws TransformationException;

}
