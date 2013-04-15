package org.webseer.transformation;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.java.JavaRuntimeFactory;
import org.webseer.java.JavaTransformation;
import org.webseer.model.meta.FileVersion;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;

public class LanguageFactory {

	private static LanguageFactory singleton;

	public static LanguageFactory getInstance() {
		if (singleton == null) {
			singleton = new LanguageFactory();
		}
		return singleton;
	}

	private final Map<String, LanguageTransformationFactory> transformations = new HashMap<String, LanguageTransformationFactory>();

	private LanguageFactory() {
		transformations.put(JavaTransformation.class.getName(), JavaRuntimeFactory.getDefaultInstance());
	}

	public Transformation generateTransformations(String language, GraphDatabaseService service,
			String name, FileVersion wrapperSource, Iterable<Library> dependencies) throws TransformationException {
		return transformations.get(language).generateTransformation(name, wrapperSource, dependencies);
	}

	public Transformation generateTransformation(String language, GraphDatabaseService service, String name, Library library,
			String identifier) throws TransformationException {
		return transformations.get(language).generateTransformation(name, library, identifier);
	}

	public Iterable<String> getTransformationLocations(String language, GraphDatabaseService service, Library library)
			throws TransformationException {
		return transformations.get(language).getTransformationLocations(library);
	}
}
