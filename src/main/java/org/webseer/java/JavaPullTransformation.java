package org.webseer.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.InputReader;
import org.webseer.transformation.OutputWriter;
import org.webseer.transformation.PullRuntimeTransformation;
import org.webseer.transformation.TransformationListener;

import com.google.common.collect.Lists;

/**
 * This wraps a JavaFunction implementation that has input and output channels and a single execute method.
 * 
 * @author ryan
 */
public abstract class JavaPullTransformation implements PullRuntimeTransformation {

	protected final Map<String, InputReader> readers = new HashMap<String, InputReader>();

	protected final Map<String, OutputWriter> outputs = new HashMap<String, OutputWriter>();

	protected final List<TransformationListener> listeners = Lists.newArrayList();

	/**
	 * For any inputs that are sinks, set them right now. Otherwise, add the streams to the list of streams.
	 */
	public void addInputChannel(String inputPoint, final InputReader inputReader) throws TransformationException {
		readers.put(inputPoint, inputReader);
	}

	@Override
	public void addOutputChannel(String outputPoint, OutputWriter output) {
		outputs.put(outputPoint, output);
	}

	@Override
	public void addListener(TransformationListener listener) {
		this.listeners.add(listener);
	}
}
