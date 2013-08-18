package org.webseer.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositoryException;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.FileVersion;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.InputType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.Type;
import org.webseer.repository.Repository;
import org.webseer.transformation.LanguageTransformationFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;

/**
 * The Java runtime factory is responsible for taking Java source from the database, compiling it, and returning a
 * runnable transformation.
 * 
 * @author ryan
 */
public class JavaRuntimeFactory implements LanguageTransformationFactory {

	private static final Logger log = LoggerFactory.getLogger(JavaRuntimeFactory.class);

	private static final JavaRuntimeFactory DEFAULT_INSTANCE = new JavaRuntimeFactory(Repository.getDefaultInstance());

	public static final JavaRuntimeFactory getDefaultInstance() {
		return DEFAULT_INSTANCE;
	}

	private static Map<Class<?>, String> PRIMITIVE_MAP = new HashMap<Class<?>, String>();

	private static Map<FieldDescriptor.Type, String> PROTOBUFFER_MAP = new HashMap<FieldDescriptor.Type, String>();

	static {
		PRIMITIVE_MAP.put(String.class, "string");
		PRIMITIVE_MAP.put(Double.class, "double");
		PRIMITIVE_MAP.put(Double.TYPE, "double");
		PRIMITIVE_MAP.put(Float.class, "float");
		PRIMITIVE_MAP.put(Float.TYPE, "float");
		PRIMITIVE_MAP.put(Integer.class, "int32");
		PRIMITIVE_MAP.put(Integer.TYPE, "int32");
		PRIMITIVE_MAP.put(Long.class, "int64");
		PRIMITIVE_MAP.put(Long.TYPE, "int64");
		PRIMITIVE_MAP.put(Boolean.class, "bool");
		PRIMITIVE_MAP.put(Boolean.TYPE, "bool");
		PRIMITIVE_MAP.put(ByteString.class, "bytes");
		PRIMITIVE_MAP.put(InputStream.class, "bytes");
		PRIMITIVE_MAP.put(OutputStream.class, "bytes");
		PRIMITIVE_MAP.put(OutputWriter.class, "bytes");

		PROTOBUFFER_MAP.put(FieldDescriptor.Type.BOOL, "bool");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.BYTES, "bytes");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.DOUBLE, "double");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.FIXED32, "int32");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.FIXED64, "int64");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.FLOAT, "float");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.INT32, "int32");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.INT64, "int64");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.SFIXED32, "int32");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.SFIXED64, "int64");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.SINT32, "int32");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.SINT64, "int64");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.STRING, "string");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.UINT32, "int32");
		PROTOBUFFER_MAP.put(FieldDescriptor.Type.UINT64, "int64");
	}

	public static final String COMPILE_DIRECTORY = "build/java";

	public static final String RUNTIME_DIRECTORY = "runtime/java";

	private final Repository repository;

	public JavaRuntimeFactory(Repository repository) {
		this.repository = repository;
	}

	public static class CompilationFailedException extends Exception {

		private static final long serialVersionUID = -6920156109590063037L;

		private List<Diagnostic<? extends JavaFileObject>> diagnostics;

		CompilationFailedException(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
			this.diagnostics = diagnostics;
		}

		public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
			return diagnostics;
		}

	}

	public Class<?> getClass(String className, String sourceAsString, Iterable<Library> dependencies)
			throws CompilationFailedException {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		Map<String, JavaFileObject> output = new HashMap<String, JavaFileObject>();

		// A loader that searches our cache first.

		RAMClassLoader loader = new RAMClassLoader(output);

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		// Create a JavaFileManager which uses our DiagnosticCollector,
		// and creates a new RAMJavaFileObject for the class, and
		// registers it in our cache

		StandardJavaFileManager sjfm = compiler.getStandardFileManager(diagnostics, Locale.getDefault(),
				Charset.defaultCharset());
		RAMFileManager jfm = new RAMFileManager(sjfm, output, loader, dependencies);

		// Create source file objects
		JavaSourceFromString src = new JavaSourceFromString(className, sourceAsString);

		CompilationTask task = compiler.getTask(null, jfm, diagnostics, null, null, Arrays.asList(src));
		if (!task.call()) {
			throw new CompilationFailedException(diagnostics.getDiagnostics());
		}

		List<String> compiledClasses = jfm.getCompiledClasses();
		if (compiledClasses.isEmpty()) {
			throw new CompilationFailedException(diagnostics.getDiagnostics());
		}

		String firstClass = compiledClasses.get(0);

		try {
			return loader.loadClass(firstClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Improper code for getting runtime class", e);
		}
	}

	static class JavaSourceFromString extends SimpleJavaFileObject {
		final String code;

		JavaSourceFromString(String name, String code) {
			super(toURI(name, Kind.SOURCE), Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}

	static URI toURI(String name, Kind kind) {
		return URI.create("string:///" + name.replace('.', '/') + kind.extension);
	}

	/**
	 * This loads from a file on the classpath. Should only be used for system classes and hopefully evenutally taken
	 * out.
	 */
	static class FileJavaFileObject extends SimpleJavaFileObject {

		File file;

		String name;

		FileJavaFileObject(String name, Kind kind, File file) {
			super(toURI(name, kind), kind);
			this.file = file;
			this.name = name;
		}

		@Override
		public InputStream openInputStream() throws IOException, IllegalStateException, UnsupportedOperationException {
			return new FileInputStream(file);
		}

	}

	/**
	 * This loads from a jar on the classpath. Should only be used for system classes and hopefully evenutally taken
	 * out.
	 */
	static class JarJavaFileObject extends SimpleJavaFileObject {

		ClassLoader loader;

		String name;

		JarJavaFileObject(String name, Kind kind, ClassLoader loader) {
			super(toURI(name, kind), kind);
			this.loader = loader;
			this.name = name;
		}

		@Override
		public InputStream openInputStream() throws IOException, IllegalStateException, UnsupportedOperationException {
			// FIXME(rrlevering): This is not kosher...the classloader is the RAM loader which can't find resources
			InputStream stream = loader.getResourceAsStream(name.replaceAll("\\.", "/") + ".class");
			return stream;
		}

	}

	/**
	 * Represents a class stored in a library in the database.
	 */
	class RepositoryJarFileObject extends SimpleJavaFileObject {

		private Library library;
		private String name;

		protected RepositoryJarFileObject(String name, Kind kind, Library library) {
			super(toURI(name, kind), kind);
			this.library = library;
			this.name = name;
		}

		@Override
		public InputStream openInputStream() throws IOException, IllegalStateException, UnsupportedOperationException {
			File jarFile;
			try {
				jarFile = repository.getArtifact(library);
			} catch (RepositoryException e) {
				throw new IOException("Problem opening repository jar");
			}
			JarInputStream jarIn = new JarInputStream(new FileInputStream(jarFile));
			try {
				JarEntry entry;
				while ((entry = jarIn.getNextJarEntry()) != null) {
					if (entry.getName().equals(name.replaceAll("\\.", "/") + ".class")) {
						byte[] classBuffer = IOUtils.toByteArray(jarIn);
						return new ByteArrayInputStream(classBuffer);
					}
				}
				return null;
			} finally {
				jarIn.close();
			}

		}

	}

	/**
	 * This is used to store the database-compiled classes in memory so other classes can reference them. This maybe
	 * needs to go away as well since we really don't support multiple files being compiled at once without a library.
	 */
	static class MemoryJavaFileObject extends SimpleJavaFileObject {

		ByteArrayOutputStream output;

		String name;

		MemoryJavaFileObject(String name, Kind kind) {
			super(toURI(name, kind), kind);
			this.name = name;
		}

		@Override
		public InputStream openInputStream() throws IOException, IllegalStateException, UnsupportedOperationException {
			return new ByteArrayInputStream(output.toByteArray());
		}

		@Override
		public OutputStream openOutputStream() throws IOException, IllegalStateException, UnsupportedOperationException {
			return output = new ByteArrayOutputStream();
		}

	}

	/**
	 * This classloader is designed to load:
	 * <ul>
	 * <li>Classes defined in webseer
	 * <li>Libraries added as dependencies
	 * <li>Special webseer annotation classes
	 * <li>Java system classes
	 * </ul>
	 */
	static final class RAMClassLoader extends ClassLoader {
		private final Map<String, JavaFileObject> output;

		RAMClassLoader(Map<String, JavaFileObject> output) {
			super();

			this.output = output;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			// Only allow the classloader to get system resources + special webseer annotations
			if (name.equals(FunctionDef.class.getPackage().getName().replace(".", "/"))) {
				Vector<URL> urls = new Vector<URL>();
				urls.add(JavaRuntimeFactory.class.getClassLoader().getResource(
						FunctionDef.class.getPackage().getName().replace(".", "/") + "/"));
				return urls.elements();
			}
			return super.getResources(name);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				return findClass(name);
			} catch (ClassNotFoundException e) {
				if (name.equals(FunctionDef.class.getName())) {
					return FunctionDef.class;
				} else if (name.equals(InputChannel.class.getName())) {
					return InputChannel.class;
				} else if (name.equals(OutputChannel.class.getName())) {
					return OutputChannel.class;
				} else if (name.equals(OutputWriter.class.getName())) {
					return OutputWriter.class;
				}
				return super.loadClass(name); // Allow any class to be loaded
			}
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			JavaFileObject jfo = output.get(name);
			log.info("Looking for " + name);
			if (jfo != null) {
				byte[] bytes;
				try {
					bytes = IOUtils.toByteArray(jfo.openInputStream());
				} catch (IOException e) {
					throw new ClassNotFoundException("Problem loading in memory class", e);
				}
				return defineClass(name, bytes, 0, bytes.length);
			}
			throw new ClassNotFoundException("Could not find class in memory loader '" + name + "'");
		}
	}

	class RAMFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

		private final Map<String, JavaFileObject> output;

		private final ClassLoader ldr;

		private Iterable<Library> dependencies;

		private List<String> compiledClasses = new ArrayList<String>();

		public RAMFileManager(StandardJavaFileManager sjfm, Map<String, JavaFileObject> output, ClassLoader ldr,
				Iterable<Library> dependencies) {
			super(sjfm);
			this.output = output;
			this.ldr = ldr;
			this.dependencies = dependencies;
		}

		public JavaFileObject getJavaFileForOutput(Location location, String name, Kind kind, FileObject sibling)
				throws IOException {
			MemoryJavaFileObject jfo = new MemoryJavaFileObject(name, kind);
			output.put(name, jfo);
			compiledClasses.add(name);
			return jfo;
		}

		public List<String> getCompiledClasses() {
			return compiledClasses;
		}

		public ClassLoader getClassLoader(Location location) {
			return ldr;
		}

		@Override
		public String inferBinaryName(Location loc, JavaFileObject jfo) {
			String result;

			if (loc == StandardLocation.CLASS_PATH && jfo instanceof MemoryJavaFileObject)
				result = ((MemoryJavaFileObject) jfo).name;

			else if (loc == StandardLocation.CLASS_PATH && jfo instanceof FileJavaFileObject)
				result = ((FileJavaFileObject) jfo).name;
			else if (loc == StandardLocation.CLASS_PATH && jfo instanceof JarJavaFileObject)
				result = ((JarJavaFileObject) jfo).name;
			else if (loc == StandardLocation.CLASS_PATH && jfo instanceof RepositoryJarFileObject)
				result = ((RepositoryJarFileObject) jfo).name;
			else
				result = super.inferBinaryName(loc, jfo);

			return result;
		}

		@Override
		public Iterable<JavaFileObject> list(Location loc, String pkg, Set<Kind> kind, boolean recurse)
				throws IOException {

			Iterable<JavaFileObject> result = super.list(loc, pkg, kind, recurse);

			// Built-ins get loaded from this file manager
			if (loc == StandardLocation.CLASS_PATH && kind.contains(Kind.CLASS)) {
				ArrayList<JavaFileObject> temp = new ArrayList<JavaFileObject>(3);
				for (JavaFileObject jfo : result)
					temp.add(jfo);
				// First add all dependencies
				for (Library dependency : dependencies) {
					List<String> classNames = getClassNames(dependency, pkg);
					for (String className : classNames) {
						RepositoryJarFileObject inMemoryJar = new RepositoryJarFileObject(className, Kind.CLASS,
								dependency);
						temp.add(inMemoryJar);
						output.put(className, inMemoryJar);
					}
				}

				// Load system dependencies
				// Only allow application classes from approved classes
				if (pkg.startsWith("java") || pkg.startsWith("org.webseer")) {

					// We need to find the class files by looking up the package as
					// a directory
					Enumeration<URL> directories = ldr.getResources(pkg.replaceAll("\\.", "/"));
					while (directories.hasMoreElements()) {
						URL directory = directories.nextElement();
						if (directory != null && directory.getProtocol().equals("file")) {
							// Get all the class files in this directory
							File dirFile;
							try {
								dirFile = new File(directory.toURI());
								for (File file : dirFile.listFiles()) {
									if (file.getName().endsWith(".class")) {
										temp.add(new FileJavaFileObject(pkg + "."
												+ file.getName().substring(0, file.getName().length() - 6), Kind.CLASS,
												file));
									}
								}
							} catch (URISyntaxException e) {
								e.printStackTrace();
							}
						} else if (directory != null) {
							String jarPath = directory.getPath();
							jarPath = jarPath.substring(5, jarPath.lastIndexOf('!'));
							JarFile jarFile = new JarFile(jarPath);
							Enumeration<JarEntry> iterator = jarFile.entries();
							while (iterator.hasMoreElements()) {
								JarEntry entry = iterator.nextElement();
								if (entry.getName().startsWith(pkg.replaceAll("\\.", "/"))) {
									String rest = entry.getName().substring(pkg.length() + 1);
									if (rest.indexOf('/') < 0 && rest.indexOf('$') < 0 && rest.endsWith(".class")) {
										temp.add(new JarJavaFileObject(
												pkg + "." + rest.substring(0, rest.length() - 6), Kind.CLASS, ldr));
									}
								}
							}
							jarFile.close();
						}
					}
				} else {
					log.info("Could not load dependencies for package " + pkg);
				}
				result = temp;
			}
			return result;
		}
	}

	List<String> getClassNames(Library library, String packageFilter) throws IOException {
		File jarFile;
		try {
			jarFile = repository.getArtifact(library);
		} catch (RepositoryException e) {
			throw new IOException("Problem opening repository jar");
		}
		List<String> names = new ArrayList<String>();
		JarInputStream jarIn = new JarInputStream(new FileInputStream(jarFile));
		try {
			JarEntry entry;
			while ((entry = jarIn.getNextJarEntry()) != null) {
				if (!entry.getName().endsWith(".class")) {
					continue;
				}
				String javaizedName = entry.getName().substring(0, entry.getName().length() - 6).replaceAll("/", ".");
				if ((packageFilter == null || javaizedName.startsWith(packageFilter)) && javaizedName.indexOf('$') < 0) {
					names.add(javaizedName);
				}
			}
		} finally {
			jarIn.close();
		}
		return names;
	}

	/**
	 * Compiles the source, using the library dependencies as the classpath.
	 */
	@Override
	public Transformation generateTransformation(String qualifiedName, FileVersion wrapperSource,
			Iterable<Library> dependencies) throws TransformationException {
		if (qualifiedName == null || qualifiedName.isEmpty()) {
			throw new TransformationException("No transformation name specified");
		}

		GraphDatabaseService service = Neo4JMetaUtils.getNode(wrapperSource).getGraphDatabase();

		String code = wrapperSource.getCode();

		Class<?> clazz;
		try {
			clazz = getClass(qualifiedName, code, dependencies);
		} catch (CompilationFailedException e) {
			throw new TransformationException(e);
		}

		Transformation trans = new JavaTransformation(service, qualifiedName, wrapperSource);
		for (Library library : dependencies) {
			Neo4JMetaUtils.getNode(trans).createRelationshipTo(Neo4JMetaUtils.getNode(library),
					NeoRelationshipType.TRANSFORMATION_LIBRARY);
		}

		FunctionDef metaInfo = clazz.getAnnotation(FunctionDef.class);
		if (metaInfo != null) {
			trans.setDescription(metaInfo.description());
			trans.setKeyWords(metaInfo.keywords());
		}

		// Get inputs and outputs
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (field.getAnnotation(InputChannel.class) != null) {
				// An input
				org.webseer.model.meta.Field transField = translateField(service, field);
				new InputPoint(service, trans, transField, InputType.SERIAL, true, transField.isRepeated());

			} else if (field.getAnnotation(OutputChannel.class) != null) {
				// An output
				org.webseer.model.meta.Field transField = translateField(service, field);
				new OutputPoint(service, trans, transField);
			}
		}

		return trans;
	}

	/**
	 * Recur through the field type until we have a tree of primitives.
	 * 
	 * @throws TransformationException
	 */
	private org.webseer.model.meta.Field translateField(GraphDatabaseService service, Field field)
			throws TransformationException {
		return translateField(service, field.getName(), field.getGenericType());
	}

	private org.webseer.model.meta.Field translateField(GraphDatabaseService service, String name,
			java.lang.reflect.Type fieldType) throws TransformationException {

		boolean repeated = false;
		Type type;
		if (fieldType instanceof ParameterizedType) {
			// Check if this is a collection
			ParameterizedType paramType = (ParameterizedType) fieldType;
			java.lang.reflect.Type rawType = paramType.getRawType();
			if (!(rawType instanceof Class)) {
				throw new TransformationException("Unsupported generic type: " + rawType);
			}
			Class<?> rawClass = (Class<?>) rawType;
			if (!Iterable.class.isAssignableFrom(rawClass) && !Iterator.class.isAssignableFrom(rawClass)
					&& !OutputWriter.class.isAssignableFrom(rawClass)) {
				throw new TransformationException("Unsupported generic type: " + rawType);
			}
			// Repeated of the generic type
			repeated = true;
			java.lang.reflect.Type genericType = paramType.getActualTypeArguments()[0];
			if (!(genericType instanceof Class)) {
				throw new TransformationException("Unsupported generic type: " + rawType);
			}
			type = translateSimpleType(service, (Class<?>) genericType);
			repeated = true;
		} else if (fieldType instanceof Class) {
			Class<?> clazz = (Class<?>) fieldType;
			if (clazz.isArray()) {
				Class<?> componentType = clazz.getComponentType();
				type = translateSimpleType(service, componentType);
				repeated = true;
			} else {
				// Simple case
				type = translateSimpleType(service, clazz);
			}
		} else {
			throw new TransformationException("Unable to interpret type" + fieldType);
		}

		return new org.webseer.model.meta.Field(service, type, name, repeated);
	}

	private Type translateSimpleType(GraphDatabaseService service, Class<?> clazz) throws TransformationException {
		if (PRIMITIVE_MAP.containsKey(clazz)) {
			return new Type(service, PRIMITIVE_MAP.get(clazz));
		} else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
			throw new TransformationException("Can't interpret interface or abstract class fields for " + clazz);
		} else if (clazz.isInstance(Message.class)) {
			// Special case handling for protos
			try {
				Descriptor descriptor = ((Message) clazz.getMethod("getDefaultInstance", new Class[0]).invoke(null))
						.getDescriptorForType();
				return translateDescriptor(service, descriptor);
			} catch (Exception e) {
				throw new TransformationException("Problem interpreting protocol buffer " + clazz);
			}
		} else {
			// Get all the fields of the class
			Type aggregateType = new Type(service, clazz.getName());
			for (Field field : clazz.getFields()) {
				if (!Modifier.isStatic(field.getModifiers())) {
					org.webseer.model.meta.Field transformField = translateField(service, field);
					aggregateType.addField(service, transformField);
				}
			}
			return aggregateType;
		}
	}

	private Type translateDescriptor(GraphDatabaseService service, Descriptor descriptor)
			throws TransformationException {
		Type aggregateType = new Type(service, descriptor.getFullName());
		for (FieldDescriptor field : descriptor.getFields()) {
			Type type;
			switch (field.getType()) {
			case ENUM:
				throw new TransformationException("Can't handle enums in protocol buffers yet");
			case MESSAGE:
				type = translateDescriptor(service, field.getMessageType());
			default:
				type = new Type(service, PROTOBUFFER_MAP.get(field.getType()));
			}
			aggregateType.addField(service,
					new org.webseer.model.meta.Field(service, type, field.getName(), field.isRepeated()));
		}
		return aggregateType;
	}

	/**
	 * Gets a list of all of the public static methods in a JAR file.
	 */
	@Override
	public Iterable<String> getTransformationLocations(Library library) throws TransformationException {
		try {
			List<File> localJars = repository.resolveArtifact(library.getGroup(), library.getName(),
					library.getVersion());

			List<URL> localUrls = new ArrayList<URL>();
			for (File localJar : localJars) {
				localUrls.add(localJar.toURI().toURL());
			}
			URLClassLoader loader = URLClassLoader.newInstance(localUrls.toArray(new URL[localUrls.size()]));

			List<String> methodNames = new ArrayList<String>();
			for (String className : getClassNames(library, null)) {
				Class<?> loadedClass = loader.loadClass(className);

				for (Method m : loadedClass.getMethods()) {
					if (Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers())) {
						methodNames.add(serializeMethod(m));
					}
				}
			}

			return methodNames;
		} catch (Exception e) {
			throw new TransformationException(e);
		}
	}

	public static String serializeMethod(Method m) {
		StringBuilder methodDescriptor = new StringBuilder(m.getDeclaringClass().getName() + "::" + m.getName() + "(");
		for (int i = 0; i < m.getParameterTypes().length; i++) {
			Class<?> paramClass = m.getParameterTypes()[i];
			if (i > 0) {
				methodDescriptor.append(",");
			}
			methodDescriptor.append(serialize(paramClass));
		}
		methodDescriptor.append(")");
		return methodDescriptor.toString();
	}

	public static String serialize(Class<?> paramClass) {
		return paramClass.getCanonicalName();
	}

	/**
	 * Generates a transformation that calls a particular static method in a JAR file.
	 */
	@Override
	public Transformation generateTransformation(String name, Library library, String identifier)
			throws TransformationException {
		GraphDatabaseService service = Neo4JMetaUtils.getNode(library).getGraphDatabase();

		try {
			Method method = getMethod(identifier, library);
			if (method == null) {
				throw new TransformationException("Could not find method");
			}

			JavaMethodTransformation transformation = new JavaMethodTransformation(service, name, library, method);

			new OutputPoint(service, transformation, translateField(service, "return", method.getGenericReturnType()));

			int i = 0;
			for (java.lang.reflect.Type paramType : method.getGenericParameterTypes()) {
				new InputPoint(service, transformation, translateField(service, "arg" + i++, paramType));
			}

			return transformation;
		} catch (Exception e) {
			throw new TransformationException(e);
		}
	}

	public Method getMethod(String identifier, Library library) throws RepositoryException, MalformedURLException,
			ClassNotFoundException, SecurityException, NoSuchMethodException {
		List<File> localJars = repository.resolveArtifact(library.getGroup(), library.getName(), library.getVersion());

		List<URL> localUrls = new ArrayList<URL>();
		for (File localJar : localJars) {
			localUrls.add(localJar.toURI().toURL());
		}
		URLClassLoader loader = URLClassLoader.newInstance(localUrls.toArray(new URL[localUrls.size()]));

		return deserializeMethod(identifier, loader);
	}

	public static Method deserializeMethod(String string, ClassLoader loader) throws ClassNotFoundException,
			SecurityException, NoSuchMethodException {
		Matcher m = Pattern.compile("(.*)\\:\\:(.*)\\((.*)\\)").matcher(string);
		m.find();

		Class<?> clazz = loader.loadClass(m.group(1));

		String[] paramStrings = m.group(3).split(",");
		Class<?>[] params = new Class<?>[paramStrings.length];
		for (int i = 0; i < paramStrings.length; i++) {
			params[i] = deserialize(paramStrings[i]);
		}
		return clazz.getMethod(m.group(2), params);
	}

	private static Class<?> deserialize(String string) throws ClassNotFoundException {
		if (string.endsWith("[]")) {
			String arrayClass = string.substring(0, string.length() - 2);
			if (arrayClass.equals("long")) {
				return long[].class;
			} else if (arrayClass.equals("byte")) {
				return byte[].class;
			} else if (arrayClass.equals("int")) {
				return int[].class;
			} else if (arrayClass.equals("short")) {
				return short[].class;
			} else if (arrayClass.equals("double")) {
				return double[].class;
			} else if (arrayClass.equals("float")) {
				return float[].class;
			} else if (arrayClass.equals("char")) {
				return char[].class;
			} else {
				return Class.forName("[L" + arrayClass + ";");
			}
		}
		if (string.equals("long")) {
			return Long.TYPE;
		} else if (string.equals("byte")) {
			return Byte.TYPE;
		} else if (string.equals("int")) {
			return Integer.TYPE;
		} else if (string.equals("short")) {
			return Short.TYPE;
		} else if (string.equals("double")) {
			return Double.TYPE;
		} else if (string.equals("float")) {
			return Float.TYPE;
		} else if (string.equals("char")) {
			return Character.TYPE;
		}
		return Class.forName(string);
	}
}
