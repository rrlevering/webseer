package org.webseer.transformation;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.Type;
import org.webseer.streams.model.runtime.RuntimeConfiguration;
import org.webseer.streams.model.runtime.RuntimeTransformationNode;

public class LanguageFactory {

	private static LanguageFactory singleton;

	public static LanguageFactory getInstance() {
		if (singleton == null) {
			singleton = new LanguageFactory();
		}
		return singleton;
	}

	private final Map<String, RuntimeFactory> languages = new HashMap<String, RuntimeFactory>();

	private LanguageFactory() {
		languages.put("Java", new JavaRuntimeFactory());
	}

	public Transformation generateTransformation(String language, GraphDatabaseService service, String qualifiedName,
			Reader reader, long version) throws IOException {
		return languages.get(language).generateTransformation(service, qualifiedName, reader, version);
	}

	public PullRuntimeTransformation generatePullTransformation(RuntimeConfiguration config,
			RuntimeTransformationNode runtime) throws TransformationException {
		return languages.get(runtime.getTransformationNode().getTransformation().getLanguage())
				.generatePullTransformation(config, runtime);
	}

	public Type generateType(String language, GraphDatabaseService service, String qualifiedName, Reader reader,
			long version) throws IOException {
		return languages.get(language).generateType(service, qualifiedName, reader, version);
	}
}
