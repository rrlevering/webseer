package org.webseer.transformation;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.java.JavaRuntimeFactory;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.UserType;
import org.webseer.type.LanguageTypeFactory;

public class LanguageFactory {

	private static LanguageFactory singleton;

	public static LanguageFactory getInstance() {
		if (singleton == null) {
			singleton = new LanguageFactory();
		}
		return singleton;
	}

	private final Map<String, String> transformationExtensions = new HashMap<String, String>();

	private final Map<String, String> typeExtensions = new HashMap<String, String>();

	private final Map<String, String> libraryExtensions = new HashMap<String, String>();

	private final Map<String, LanguageTransformationFactory> transformations = new HashMap<String, LanguageTransformationFactory>();

	private final Map<String, LanguageTypeFactory> types = new HashMap<String, LanguageTypeFactory>();

	private LanguageFactory() {
		JavaRuntimeFactory java = new JavaRuntimeFactory();
		transformations.put("Java", java);
		types.put("Java", java);
		transformationExtensions.put("java", "Java");
		typeExtensions.put("java", "Java");
		libraryExtensions.put("jar", "Java");
	}

	public Transformation generateTransformation(String language, GraphDatabaseService service, String qualifiedName,
			Reader reader, long version) throws IOException {
		return transformations.get(language).generateTransformation(service, qualifiedName, reader, version);
	}

	public PullRuntimeTransformation generatePullTransformation(Transformation transformation)
			throws TransformationException {
		return transformations.get(transformation.getLanguage()).generatePullTransformation(transformation);
	}

	public UserType generateType(String language, GraphDatabaseService service, String qualifiedName, Reader reader,
			long version) throws IOException {
		return types.get(language).generateType(service, qualifiedName, reader, version);
	}

	public Library generateLibrary(String language, GraphDatabaseService service, String packageName,
			String libraryName, InputStream stream, String version) throws IOException {
		return transformations.get(language).generateLibrary(service, packageName, libraryName, version, stream);
	}

	public String getLanguageForTransformationExtension(String extension) {
		return transformationExtensions.get(extension);
	}

	public String getLanguageForLibraryExtension(String extension) {
		return libraryExtensions.get(extension);
	}

	public String getLanguageForTypeExtension(String extension) {
		return typeExtensions.get(extension);
	}
}
