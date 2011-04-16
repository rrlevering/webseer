package org.webseer.transformation;

import java.util.HashMap;
import java.util.Map;

import org.webseer.model.meta.TransformationException;
import org.webseer.model.runtime.RuntimeConfiguration;
import org.webseer.model.runtime.RuntimeTransformationNode;

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

	public PullRuntimeTransformation generatePullTransformation(RuntimeConfiguration config,
			RuntimeTransformationNode runtime) throws TransformationException {
		return languages.get(runtime.getTransformationNode().getTransformation().getLanguage())
				.generatePullTransformation(config, runtime);
	}
}
