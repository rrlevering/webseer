package org.webseer.java;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.InputReader;
import org.webseer.transformation.OutputWriter;
import org.webseer.transformation.RuntimeTransformationException;
import org.webseer.transformation.TransformationListener;

public class MethodTransformation extends JavaPullTransformation {

	private static final Logger log = LoggerFactory.getLogger(MethodTransformation.class);

	private final Method toRun;
	private final Object object;
	private final Transformation transformation;

	public MethodTransformation(Transformation transformation, Method toRun, Object object) {
		this.toRun = toRun;
		this.object = object;
		this.transformation = transformation;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean transform() throws TransformationException, RuntimeTransformationException {
		// Start new transformation group
		for (TransformationListener listener : listeners) {
			listener.init();
		}

		Object[] arguments = new Object[toRun.getParameterTypes().length];

		// Pull the inputs into arguments
		Class<?>[] params = toRun.getParameterTypes();
		int i = 0;
		for (InputPoint inputPoint : transformation.getInputPoints()) {
			final InputReader reader = readers.get(inputPoint.getName());
			Class<?> param = params[i];
			
			if (param.isAssignableFrom(Iterable.class)) {
				log.info("Adding iterable input for {}", inputPoint.getName());
				arguments[i] = new Iterable<Object>() {

					@Override
					public Iterator<Object> iterator() {
						return reader.getInputStream();
					}
					
				};
			} else {
				Object inputObject = reader.getInputStream().next();
				log.info("Input for {} = {}", inputPoint.getName(), inputObject);
				arguments[i] = inputObject;
			}
			i++;
		}

		for (TransformationListener listener : listeners) {
			listener.start();
		}

		Object returnValue;
		try {
			returnValue = toRun.invoke(object, arguments);
		} catch (IllegalArgumentException e) {
			throw new TransformationException("Problem invoking method", e);
		} catch (IllegalAccessException e) {
			throw new TransformationException("Problem invoking method", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeTransformationException(e);
		}

		for (TransformationListener listener : listeners) {
			listener.end();
		}

		log.info("Output = {}", returnValue);

		// Add the return value to output listeners
		if (returnValue != null) {
			@SuppressWarnings("rawtypes")
			OutputWriter valueList = outputs.get("return");
			if (valueList != null) {
				if (returnValue instanceof Iterable) {
					for (Object object : ((Iterable<?>) returnValue)) {
						valueList.writeObject(object);
					}
				} else if (!(returnValue instanceof OutputStream)) {
					valueList.writeObject(returnValue);
				}
			}
		}

		return true;
	}
}
