package org.webseer.transformation;

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

import org.webseer.model.meta.TransformationException;
import org.webseer.streams.model.runtime.RuntimeConfiguration;
import org.webseer.streams.model.runtime.RuntimeTransformationException;
import org.webseer.streams.model.runtime.RuntimeTransformationNode;

/**
 * This wraps a JavaFunction implementation that has input and output channels and a single execute method.
 * 
 * @author ryan
 */
public class PullJavaFunction implements PullRuntimeTransformation {

	private final JavaFunction object;

	private final Map<String, List<ItemInputStream>> inputs = new HashMap<String, List<ItemInputStream>>();

	private final Map<String, InputReader> readers = new HashMap<String, InputReader>();

	private final Map<String, ItemOutputStream<?>> outputs = new HashMap<String, ItemOutputStream<?>>();

	private RuntimeTransformationNode node;

	private RuntimeConfiguration config;

	private boolean runOnce = false;

	public PullJavaFunction(RuntimeConfiguration config, RuntimeTransformationNode node, JavaFunction object) {
		this.object = object;
		this.node = node;
		this.config = config;
	}

	/**
	 * For any inputs that are sinks, set them right now. Otherwise, add the streams to the list of streams.
	 */
	public void addInputChannel(String inputPoint, final InputReader inputReader) throws TransformationException {
		readers.put(inputPoint, inputReader);
	}

	@SuppressWarnings("unchecked")
	public boolean transform() throws TransformationException, RuntimeTransformationException {
		boolean needsAdvance = false;
		if (!runOnce && inputs.isEmpty()) {
			needsAdvance = true;
		} else {
			// Try a new input
			for (Entry<String, List<ItemInputStream>> entry : inputs.entrySet()) {
				for (ItemInputStream vararg : entry.getValue()) {
					if (vararg.hasNext()) {
						System.out.println("More in " + entry.getKey());
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
		this.config.initRunning(node);

		// Pull inputs, doing source synchronization
		Class<? extends JavaFunction> objectClass = object.getClass();
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
					// Class<?> rawClass = (Class<?>) ((ParameterizedType) ((GenericArrayType) type)
					// .getGenericComponentType()).getRawType();
					// Object[] streams = (Object[]) Array.newInstance(rawClass, entry.getValue().size());
					// for (int i = 0; i < entry.getValue().size(); i++) {
					// if (entry.getValue().get(i).hasNext()) {
					// streams[i] = entry.getValue().get(i).next();
					// }
					// }
					// f.set(current, streams);
				} else if (type.equals(InputStream.class)) {
					ItemInputStream inputStream = readers.get(input).getInputStream();
					inputStream.nextItem();
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
					ItemInputStream currentStream = inputs.containsKey(input) ? inputs.get(input).get(0) : null;
					if (currentStream == null) {
						currentStream = readers.get(input).getInputStream();
						List<ItemInputStream> list = new ArrayList<ItemInputStream>();
						list.add(currentStream);
						inputs.put(input, list);
					}
					if (currentStream.hasNext()) {
						System.out.println("Setting " + f.getName());
						f.set(current, currentStream.next());
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
				if (BucketOutputStream.class.isAssignableFrom(f.getType())) {
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

		this.config.startRunning(node);

		try {
			object.execute();
		} catch (Throwable e) {
			throw new RuntimeTransformationException(e);
		}

		this.config.endRunning(node);

		System.out.println("Transformed " + node.getTransformationNode().getTransformation().getName());
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
			System.out.println("Output = " + value);

			if (value != null) {
				@SuppressWarnings("rawtypes")
				ItemOutputStream valueList = outputs.get(outputPoint);
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

	@Override
	public void addOutputChannel(String outputPoint, ItemOutputStream<?> output) {
		outputs.put(outputPoint, output);
	}
}
