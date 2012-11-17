package org.webseer.visitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the main magic class of the model library. It does reflective, runtime dispatch of visit/traverse methods.
 * When the object is first created, the methods in the object are reflectively checked and method calling objects are
 * created and hashed to the class that they visit. Whenever a visit method is called, first the hash is checked and if
 * a method is not found, the superclass/interface tree is walked up to check for the nearest compatible match. This
 * match (or no match) is then cached for future calls.
 * 
 * @author Ryan Levering
 */
public class ReflectiveSuperVisitor extends AbstractStoppableVisitor implements SuperVisitor {

	protected static final DirectLoader loader = new DirectLoader();

	private static final Map<Class<?>, Map<Class<?>, MethodCaller>> visitorCache = new HashMap<Class<?>, Map<Class<?>, MethodCaller>>();;

	private static final Map<Class<?>, Map<Class<?>, MethodCaller>> preVisitorCache = new HashMap<Class<?>, Map<Class<?>, MethodCaller>>();

	private static final Map<Class<?>, Map<Class<?>, MethodCaller>> postVisitorCache = new HashMap<Class<?>, Map<Class<?>, MethodCaller>>();

	private static final Map<Class<?>, Map<Class<?>, MethodCaller>> preTravelerCache = new HashMap<Class<?>, Map<Class<?>, MethodCaller>>();

	private static final Map<Class<?>, Map<Class<?>, MethodCaller>> postTravelerCache = new HashMap<Class<?>, Map<Class<?>, MethodCaller>>();

	protected final Map<Class<?>, MethodCaller> visitors;

	protected final Map<Class<?>, MethodCaller> preVisitors;

	protected final Map<Class<?>, MethodCaller> postVisitors;

	protected final Map<Class<?>, MethodCaller> postTravelers;

	protected final Map<Class<?>, MethodCaller> preTravelers;

	/**
	 * Creates a new reflection-based visitor that uses dynamic runtime dispatch to find the correct method to call on
	 * the passed arguments. This will initialize the method caches with the declared argument types.
	 */
	public ReflectiveSuperVisitor() {
		if (visitorCache.containsKey(getClass())) {
			this.visitors = visitorCache.get(getClass());
			this.preVisitors = preVisitorCache.get(getClass());
			this.postVisitors = postVisitorCache.get(getClass());
			this.preTravelers = preTravelerCache.get(getClass());
			this.postTravelers = postTravelerCache.get(getClass());
		} else {
			this.visitors = new HashMap<Class<?>, MethodCaller>();
			this.preVisitors = new HashMap<Class<?>, MethodCaller>();
			this.postVisitors = new HashMap<Class<?>, MethodCaller>();
			this.postTravelers = new HashMap<Class<?>, MethodCaller>();
			this.preTravelers = new HashMap<Class<?>, MethodCaller>();

			Method[] methods = getClass().getMethods();
			for (Method method : methods) {
				if (!Modifier.isAbstract(method.getModifiers()) && !method.getDeclaringClass().isInterface()
						&& !method.getDeclaringClass().equals(ReflectiveSuperVisitor.class)) {
					if (method.getName().equals("visit") && method.getParameterTypes().length == 1) {
						if (!method.getParameterTypes()[0].equals(Acceptor.class)
								&& !method.getParameterTypes()[0].equals(Object.class)) {
							this.visitors.put(method.getParameterTypes()[0], createCaller(method));
						}
					} else if (method.getName().equals("preVisit") && method.getParameterTypes().length == 1) {
						if (!method.getParameterTypes()[0].equals(Acceptor.class)
								&& !method.getParameterTypes()[0].equals(Object.class)) {
							this.preVisitors.put(method.getParameterTypes()[0], createCaller(method));
						}
					} else if (method.getName().equals("postVisit") && method.getParameterTypes().length == 1) {
						if (!method.getParameterTypes()[0].equals(Acceptor.class)
								&& !method.getParameterTypes()[0].equals(Object.class)) {
							this.postVisitors.put(method.getParameterTypes()[0], createCaller(method));
						}
					} else if (method.getName().equals("preTraverse") && method.getParameterTypes().length == 1) {
						if (!method.getParameterTypes()[0].equals(Acceptor.class)
								&& !method.getParameterTypes()[0].equals(Object.class)) {
							this.preTravelers.put(method.getParameterTypes()[0], createCaller(method));
						}
					} else if (method.getName().equals("postTraverse") && method.getParameterTypes().length == 1) {
						if (!method.getParameterTypes()[0].equals(Acceptor.class)
								&& !method.getParameterTypes()[0].equals(Object.class)) {
							this.postTravelers.put(method.getParameterTypes()[0], createCaller(method));
						}
					}
				}
			}
			visitorCache.put(getClass(), this.visitors);
			preTravelerCache.put(getClass(), this.preTravelers);
			postTravelerCache.put(getClass(), this.postTravelers);
			preVisitorCache.put(getClass(), this.preVisitors);
			postVisitorCache.put(getClass(), this.postVisitors);
		}
	}

	/**
	 * This is a convenience method to check whether this visitor has any handling code for a particular class.
	 * 
	 * @param dataClass any Acceptor subclass that might be handled by this visitor
	 * @return true if the visitor has handling code written for the class argument or any of its
	 *         superclasses/interfaces
	 */
	public boolean canVisit(Class<? extends Object> dataClass) {
		return getVisitor(dataClass) != null;
	}

	public void postTraverse(Object acceptor) throws VisitorException {
		call(this.postTravelers, acceptor);
	}

	public void postVisit(Object acceptor) throws VisitorException {
		call(this.postVisitors, acceptor);
	}

	public void preTraverse(Object acceptor) throws VisitorException {
		call(this.preTravelers, acceptor);
	}

	public void preVisit(Object acceptor) throws VisitorException {
		call(this.preVisitors, acceptor);
	}

	public void visit(Object acceptor) throws VisitorException {
		call(this.visitors, acceptor);
	}

	protected final void call(Map<Class<?>, MethodCaller> methodMap, Object acceptor) {
		if (acceptor == null) {
			return;
		}
		if (!isStopped()) {
			MethodCaller visitor = getMethod(methodMap, acceptor);
			if (visitor != null) {
				visitor.callWith(this, acceptor);
			}
		}
	}

	protected synchronized final MethodCaller getMethod(Map<Class<?>, MethodCaller> methodMap, Object acceptor) {
		if (methodMap.containsKey(acceptor.getClass())) {
			return methodMap.get(acceptor.getClass());
		} else {
			MethodCaller visitor = findVisitForClass(methodMap, acceptor.getClass());
			methodMap.put(acceptor.getClass(), visitor);
			return visitor;
		}
	}

	private MethodCaller createCaller(Method m) {
		// Old way
		return new SimpleCaller(m);

		// Faster way, but has some problems on servlet classpaths
		// String className = getClass().getName() + m.getName() + m.getParameterTypes()[0].getSimpleName();
		// Class<?> clazz;
		// synchronized (loader) {
		// try {
		// clazz = loader.loadClass(className);
		// } catch (ClassNotFoundException e) {
		// try {
		// ClassPool pool = ClassPool.getDefault();
		// CtClass byteCodeInvoker;
		// try {
		// byteCodeInvoker = pool.get(className);
		// } catch (NotFoundException g) {
		// byteCodeInvoker = pool.makeClass(className);
		// byteCodeInvoker.addInterface(pool.get(MethodCaller.class.getName()));
		//
		// CtConstructor constructor = new CtConstructor(new CtClass[] {}, byteCodeInvoker);
		// constructor.setBody(";");
		// byteCodeInvoker.addConstructor(constructor);
		//
		// CtMethod method = new CtMethod(CtClass.voidType, "callWith", new CtClass[] {
		// pool.get(ReflectiveSuperVisitor.class.getName()), pool.get(Object.class.getName()) },
		// byteCodeInvoker);
		//
		// method.setBody("((" + getClass().getName() + ") $1)." + m.getName() + "(("
		// + m.getParameterTypes()[0].getName() + ") $2);");
		// byteCodeInvoker.addMethod(method);
		// }
		//
		// clazz = loader.load(className, byteCodeInvoker.toBytecode());
		//
		// } catch (Throwable f) {
		// throw new RuntimeException("Unable to create byte code visitor invoker", f);
		// }
		// }
		// }
		// try {
		// return (MethodCaller) clazz.newInstance();
		// } catch (Exception e) {
		// throw new RuntimeException("Unable to instantiate method caller class");
		// }
	}

	private MethodCaller findVisitForClass(Map<Class<?>, MethodCaller> visitors, Class<?> dataClass) {
		List<Class<?>> ancestors = new ArrayList<Class<?>>();
		ancestors.add(dataClass);
		while (!ancestors.isEmpty()) {
			Class<?> ancestorClass = ancestors.remove(0);
			if (visitors.containsKey(ancestorClass)) {
				return visitors.get(ancestorClass);
			}
			if (ancestorClass.getSuperclass() != null) {
				ancestors.add(ancestorClass.getSuperclass());
			}
			for (Class<?> interfaceClass : ancestorClass.getInterfaces()) {
				ancestors.add(interfaceClass);
			}
		}
		return null;

	}

	private <T extends Object> MethodCaller getVisitor(Class<T> dataClass) {
		if (this.visitors.containsKey(dataClass)) {
			return this.visitors.get(dataClass);
		}
		MethodCaller visitor = findVisitForClass(this.visitors, dataClass);
		this.visitors.put(dataClass, visitor);
		return visitor;
	}

	public static interface MethodCaller {

		public void callWith(ReflectiveSuperVisitor visitor, Object source);

	}

	protected static class DirectLoader extends SecureClassLoader {

		protected DirectLoader() {
			super(ReflectiveSuperVisitor.class.getClassLoader());
		}

		protected Class<?> load(String name, byte[] data) {
			try {
				return super.loadClass(name);
			} catch (ClassNotFoundException e) {
				return super.defineClass(name, data, 0, data.length);
			}
		}

	}

	/*
	 * This is left here for backporting.
	 */
	private final class SimpleCaller implements MethodCaller {

		private final Method visitMethod;

		private SimpleCaller(Method visitMethod) {
			this.visitMethod = visitMethod;
			this.visitMethod.setAccessible(true);
		}

		public void callWith(ReflectiveSuperVisitor visitor, Object source) {
			try {
				if (this.visitMethod.getParameterTypes().length == 1) {
					this.visitMethod.invoke(visitor, new Object[] { source });
				} else {
					this.visitMethod.invoke(visitor, new Object[] {});
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

	}

}