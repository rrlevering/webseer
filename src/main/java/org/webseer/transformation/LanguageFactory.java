package org.webseer.transformation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.java.JavaRuntimeFactory;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.Type;
import org.webseer.proto.ProtocolBufferFactory;
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
		
		ProtocolBufferFactory proto = new ProtocolBufferFactory();
		types.put("Proto", proto);
		typeExtensions.put("proto", "Proto");
	}

	public Collection<Transformation> generateTransformations(String language, GraphDatabaseService service, String qualifiedName,
			InputStream reader, long version) throws IOException, TransformationException {
		return transformations.get(language).generateTransformations(service, qualifiedName, reader, version);
	}

	public PullRuntimeTransformation generatePullTransformation(Transformation transformation)
			throws TransformationException {
		return transformations.get(transformation.getLanguage()).generatePullTransformation(transformation);
	}

	public Collection<Type> generateTypes(String language, GraphDatabaseService service, String qualifiedName, InputStream reader,
			long version) throws IOException, TransformationException {
		return types.get(language).generateTypes(service, qualifiedName, reader, version);
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
