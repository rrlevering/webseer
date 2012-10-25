package org.webseer.transformation.java;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.PushRuntimeTransformation;
import org.webseer.visitor.AggregateSuperVisitor;
import org.webseer.visitor.ReflectiveSuperVisitor;
import org.webseer.visitor.SuperVisitor;

public abstract class JavaTransformation implements PushRuntimeTransformation {

	private static final Logger log = LoggerFactory.getLogger(JavaTransformation.class);

	private Map<String, AggregateSuperVisitor<SuperVisitor>> listeners = new HashMap<String, AggregateSuperVisitor<SuperVisitor>>();

	protected JavaTransformation() {
		// Add visitors to the output channels
		for (Field f : getClass().getDeclaredFields()) {
			if (f.isAnnotationPresent(OutputChannel.class)) {
				if (f.getType().isAssignableFrom(SuperVisitor.class)) {
					f.setAccessible(true);
					try {
						f.set(this, new AggregateSuperVisitor<SuperVisitor>());
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				} else {
					listeners.put(f.getName(), new AggregateSuperVisitor<SuperVisitor>());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void addOutputChannel(String outputPoint, SuperVisitor listener) {
		// Use the annotations to check all the fields
		Field f;
		try {
			f = getClass().getDeclaredField(outputPoint);
			f.setAccessible(true);
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new RuntimeException("Security problem getting field", e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("No input channel for that input point", e);
		}
		AggregateSuperVisitor<SuperVisitor> channel;
		if (f.getType().isAssignableFrom(SuperVisitor.class)) {
			try {
				channel = (AggregateSuperVisitor<SuperVisitor>) f.get(this);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		} else {
			channel = listeners.get(outputPoint);
		}
		channel.addVisitor(listener);
	}

	public SuperVisitor getInputChannel(String inputPoint) {
		// Use the annotations to check all the fields
		Field f;
		try {
			f = getClass().getDeclaredField(inputPoint);
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new RuntimeException("Security problem getting field", e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("No input channel for input point '" + inputPoint + "'");
		}
		if (f.getAnnotation(InputChannel.class) != null) {
			if (SuperVisitor.class.isAssignableFrom(f.getType())) {
				try {
					f.setAccessible(true);
					return (SuperVisitor) f.get(this);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				return new ReflectionSetter(f);
			}
		}
		throw new RuntimeException("No input channel for that input point");
	}

	public void postTransform() throws TransformationException {
		postInputs();
		// Copy fields out
		for (Field f : getClass().getDeclaredFields()) {
			f.setAccessible(true);
			if (f.isAnnotationPresent(OutputChannel.class)) {
				if (!f.getType().isAssignableFrom(SuperVisitor.class)) {
					try {
						listeners.get(f.getName()).visit(f.get(this));
					} catch (IllegalArgumentException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	public void preTransform() throws TransformationException {
		preInputs();
	}

	public void postInputs() throws TransformationException {
		// Do nothing
	}

	public void preInputs() throws TransformationException {
		// Do nothing
	}

	public class ReflectionSetter extends ReflectiveSuperVisitor {

		private final Field field;

		public ReflectionSetter(Field f) {
			this.field = f;
			this.field.setAccessible(true);
		}

		public void visit(Object o) {
			if (o != null) {
				try {
					if (field.getType().isAssignableFrom(o.getClass())) {
						field.set(JavaTransformation.this, o);
					} else if (o instanceof String) {
						if (field.getType().equals(Integer.TYPE)) {
							field.setInt(JavaTransformation.this, Integer.parseInt((String) o));
						}
					} else {
						log.debug("Cannot assign object of type %1 to field of type %2", o.getClass(), field.getType());
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
