package org.webseer.java;

import java.io.InputStream;
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
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.OutputWriter;
import org.webseer.transformation.RuntimeTransformationException;
import org.webseer.transformation.TransformationListener;

public class ClassTransformation extends JavaPullTransformation {

	private static final Logger log = LoggerFactory.getLogger(ClassTransformation.class);

	private final Map<String, List<Iterator<Object>>> inputs = new HashMap<String, List<Iterator<Object>>>();

	private final Object object;
	
	private boolean runOnce = false;

	public ClassTransformation(Object object) {
		this.object = object;
	}

	@SuppressWarnings("unchecked")
	public boolean transform() throws TransformationException, RuntimeTransformationException {
		boolean needsAdvance = false;
		if (!runOnce && inputs.isEmpty()) {
			needsAdvance = true;
		} else {
			// Try a new input
			for (Entry<String, List<Iterator<Object>>> entry : inputs.entrySet()) {
				for (Iterator<Object> vararg : entry.getValue()) {
					if (vararg.hasNext()) {
						log.info("More in %0", entry.getKey());
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
				} else if (type.equals(InputStream.class)) {
					Iterator<Object> inputStream = readers.get(input).getInputStream();
					inputStream.next();
					f.set(current, inputStream);
				} else if (type instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType) type;
					if (Iterator.class.isAssignableFrom((Class<?>) paramType.getRawType())) {
						f.set(current, readers.get(input).getInputStream());
					} else if (Iterable.class.isAssignableFrom((Class<?>) paramType.getRawType())) {
						f.set(current, new Iterable<Object>() {

							@Override
							public Iterator<Object> iterator() {
								return readers.get(input).getInputStream();
							}

						});
					}
				} else {
					Iterator<Object> currentStream = inputs.containsKey(input) ? inputs.get(input).get(0) : null;
					if (currentStream == null) {
						currentStream = readers.get(input).getInputStream();
						List<Iterator<Object>> list = new ArrayList<Iterator<Object>>();
						list.add(currentStream);
						inputs.put(input, list);
					}
					if (currentStream.hasNext()) {
						Object value = currentStream.next();
						log.info("Setting {} to {}", f.getName(), value);
						f.set(current, value);
					}
				}
			} catch (SecurityException e) {
				throw new TransformationException("Problem setting fields", e);
			} catch (IllegalArgumentException e) {
				throw new TransformationException("Problem setting fields", e);
			} catch (NoSuchFieldException e) {
				throw new TransformationException("Problem setting fields", e);
			} catch (IllegalAccessException e) {
				throw new TransformationException("Problem setting fields", e);
			} catch (InstantiationException e) {
				throw new TransformationException("Problem setting fields", e);
			}
		}
		// Set any output streams in the output fields
		objectClass = object.getClass();
		for (String outputPoint : outputs.keySet()) {
			try {
				Field f = objectClass.getDeclaredField(outputPoint);
				if (OutputWriter.class.isAssignableFrom(f.getType())) {
					f.setAccessible(true);
					f.set(object, outputs.get(outputPoint));
				} else if (!f.getType().isPrimitive()) {
					f.setAccessible(true);
					f.set(object, null);
				}
			} catch (SecurityException e) {
				throw new TransformationException("Problem setting fields", e);
			} catch (IllegalArgumentException e) {
				throw new TransformationException("Problem setting fields", e);
			} catch (NoSuchFieldException e) {
				throw new TransformationException("Problem setting fields", e);
			} catch (IllegalAccessException e) {
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
				f.setAccessible(true);
				value = f.get(object);
			} catch (SecurityException e) {
				throw new TransformationException("Problem getting fields", e);
			} catch (IllegalArgumentException e) {
				throw new TransformationException("Problem getting fields", e);
			} catch (NoSuchFieldException e) {
				throw new TransformationException("Problem getting fields", e);
			} catch (IllegalAccessException e) {
				throw new TransformationException("Problem getting fields", e);
			}
			log.info("Output = {}", value);

			if (value != null) {
				@SuppressWarnings("rawtypes")
				OutputWriter valueList = outputs.get(outputPoint);
				if (value instanceof Iterable) {
					for (Object object : ((Iterable<?>) value)) {
						valueList.writeObject(object);
					}
				} else if (!(value instanceof OutputStream)) {
					valueList.writeObject(value);
				}
			}
		}

		return true;
	}
}
