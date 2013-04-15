package org.webseer.transformation;

import org.webseer.model.meta.FileVersion;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;

/**
 * A factory that handles compiling sources and generating runtime transformation endpoints for webseer services.
 */
public interface LanguageTransformationFactory {

	/**
	 * This is the compile-time abstraction for a given language. It takes a reader and readers for all of the
	 * dependencies of the given transformation and returns a parsed transformation.
	 * 
	 * @throws TransformationException
	 *             if there is a problem compiling the resource
	 */
	public Transformation generateTransformation(String name, FileVersion wrapperSource, Iterable<Library> dependencies)
			throws TransformationException;

	public Iterable<String> getTransformationLocations(Library library) throws TransformationException;

	public Transformation generateTransformation(String name, Library library, String identifier) throws TransformationException;

}
