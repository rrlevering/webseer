package org.webseer.transformation;

import org.webseer.model.meta.TransformationException;
import org.webseer.model.runtime.RuntimeConfiguration;
import org.webseer.model.runtime.RuntimeTransformationNode;

public interface RuntimeFactory {

	public PullRuntimeTransformation generatePullTransformation(RuntimeConfiguration config,
			RuntimeTransformationNode runtime) throws TransformationException;

}
