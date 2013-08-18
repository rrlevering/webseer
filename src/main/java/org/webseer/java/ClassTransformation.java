package org.webseer.java;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webseer.bucket.Data;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.OutputWriter;
import org.webseer.transformation.RuntimeTransformationException;
import org.webseer.transformation.TransformationListener;

public class ClassTransformation extends JavaPullTransformation {

	private static final Logger log = LoggerFactory.getLogger(ClassTransformation.class);

	private final Map<String, List<Iterator<? extends Data>>> inputs = new HashMap<String, List<Iterator<? extends Data>>>();

	private final Object object;

	private boolean runOnce = false;

	public ClassTransformation(Object object) {
		this.object = object;
	}

	public boolean transform() throws TransformationException, RuntimeTransformationException {
		boolean needsAdvance = false;
		if (!runOnce && inputs.isEmpty()) {
			needsAdvance = true;
		} else {
			// Try a new input
			for (Entry<String, List<Iterator<? extends Data>>> entry : inputs.entrySet()) {
				for (Iterator<? extends Data> vararg : entry.getValue()) {
					if (vararg.hasNext()) {
						log.info("More in {}", entry.getKey());
						needsAdvance = true;
					}
				}
			}
		}
		if (needsAdvance) {
			runOnce = true;
		} else {
			return false;
		}

		// Start new transformation group
		for (TransformationListener listener : listeners) {
			listener.init();
		}

		// Pull inputs, doing source synchronization
		Class<? extends Object> objectClass = object.getClass();
		for (final String input : readers.keySet()) {
			String[] path = input.split("\\.");
			Field f = null;
			Class<?> valueClass = objectClass;
			Object current = object;
			try {
				for (int i = 0; i < path.length; i++) {
					f = valueClass.getDeclaredField(path[i]);
					if (i + 1 < path.length) {
						// Have more, need to possibly create
						Object parent = current;
						current = f.get(current);
						if (current == null) {
							current = f.getType().newInstance();
							f.set(parent, current);
						}
						valueClass = current.getClass();
					}
				}
				f.setAccessible(true);
				Type type = f.getGenericType();
				if (type instanceof GenericArrayType) {
					// Class<?> rawClass = (Class<?>) ((ParameterizedType)
					// ((GenericArrayType) type)
					// .getGenericComponentType()).getRawType();
					// Object[] streams = (Object[]) Array.newInstance(rawClass,
					// entry.getValue().size());
					// for (int i = 0; i < entry.getValue().size(); i++) {
					// if (entry.getValue().get(i).hasNext()) {
					// streams[i] = entry.getValue().get(i).next();
					// }
					// }
					// f.set(current, streams);
				} else if (type instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType) type;
					if (Iterator.class.isAssignableFrom((Class<?>) paramType.getRawType())) {
						f.set(current,
								JavaTypeTranslator.convertData(readers.get(input),
										(Class<?>) paramType.getActualTypeArguments()[0]));
					} else if (Iterable.class.isAssignableFrom((Class<?>) paramType.getRawType())) {
						f.set(current,
								JavaTypeTranslator.convertData(readers.get(input),
										(Class<?>) paramType.getActualTypeArguments()[0]));
					}
				} else {
					Iterator<? extends Data> currentStream = inputs.containsKey(input) ? inputs.get(input).get(0)
							: null;
					if (currentStream == null) {
						currentStream = readers.get(input).iterator();
						List<Iterator<? extends Data>> list = new ArrayList<Iterator<? extends Data>>();
						list.add(currentStream);
						inputs.put(input, list);
					}
					if (currentStream.hasNext()) {
						Object value = JavaTypeTranslator.convertData(currentStream.next(), (Class<?>) type);
						log.info("Setting {} to {}", f.getName(), value);
						f.set(current, value);
					}
				}
			} catch (SecurityException | IllegalArgumentException | NoSuchFieldException | IllegalAccessException
					| InstantiationException e) {
				throw new TransformationException("Problem setting fields", e);
			}
		}
		// Set any output streams in the output fields
		objectClass = object.getClass();
		for (final String outputPoint : outputs.keySet()) {
			try {
				Field f = objectClass.getDeclaredField(outputPoint);
				if (org.webseer.java.OutputWriter.class.isAssignableFrom(f.getType())) {
					f.setAccessible(true);
					f.set(object, new org.webseer.java.OutputWriter<Object>() {

						@Override
						public void write(Object data) {
							outputs.get(outputPoint).writeData(JavaTypeTranslator.convertObject(data));
						}

					});
				} else if (!f.getType().isPrimitive()) {
					f.setAccessible(true);
					f.set(object, null);
				}
			} catch (SecurityException | IllegalArgumentException | NoSuchFieldException | IllegalAccessException e) {
				throw new TransformationException("Problem setting fields", e);
			}
		}

		for (TransformationListener listener : listeners) {
			listener.start();
		}

		try {
			objectClass.getMethod("execute").invoke(object);
		} catch (Throwable e) {
			throw new RuntimeTransformationException(e);
		}

		for (TransformationListener listener : listeners) {
			listener.end();
		}

		objectClass = object.getClass();
		for (String outputPoint : outputs.keySet()) {
			Object value;
			try {
				Field f = objectClass.getDeclaredField(outputPoint);
				if (org.webseer.java.OutputWriter.class.isAssignableFrom(f.getType())) {
					// We've already directly written to the field, keep going
					continue;
				}
				f.setAccessible(true);
				value = f.get(object);
			} catch (SecurityException | IllegalArgumentException | NoSuchFieldException | IllegalAccessException e) {
				throw new TransformationException("Problem getting fields", e);
			}
			log.info("Output = {}", value);

			if (value != null) {
				OutputWriter valueList = outputs.get(outputPoint);
				if (value instanceof Iterable) {
					for (Object object : ((Iterable<?>) value)) {
						valueList.writeData(JavaTypeTranslator.convertObject(object));
					}
				} else if (!(value instanceof OutputStream)) {
					valueList.writeData(JavaTypeTranslator.convertObject(value));
				}
			}
		}

		return true;
	}
}
