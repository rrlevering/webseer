package org.webseer.transformation.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import javax.tools.JavaFileManager;
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
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.InputType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.TransformationField;
import org.webseer.model.meta.Type;
import org.webseer.transformation.FunctionDef;
import org.webseer.transformation.InputChannel;
import org.webseer.transformation.LibraryFactory;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.OutputWriter;
import org.webseer.transformation.PullRuntimeTransformation;
import org.webseer.transformation.RuntimeFactory;
import org.webseer.type.TypeFactory;

import com.google.protobuf.ByteString;

/**
 * The Java runtime factory is responsible for taking Java source from the database, compiling it, and returning a
 * runnable transformation.
 * 
 * @author ryan
 */
public class JavaRuntimeFactory implements RuntimeFactory {

	private static final Logger log = LoggerFactory.getLogger(JavaRuntimeFactory.class);

	private static Map<Class<?>, String> PRIMITIVE_MAP = new HashMap<Class<?>, String>();

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
	}

	public static final String COMPILE_DIRECTORY = "build/java";

	public static final String RUNTIME_DIRECTORY = "runtime/java";

	public PullRuntimeTransformation generatePullTransformation(Transformation transformation)
			throws TransformationException {

		// Write out the Java source
		String className = transformation.getName();
		String code = transformation.getCode();

		JavaFunction object;
		try {
			Class<?> clazz = getClass(className, new StringReader(code), transformation.getLibraries());
			object = (JavaFunction) clazz.newInstance();
		} catch (InstantiationException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (IllegalAccessException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (IOException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		}

		// Load and wrap the object with a java transformation wrapper
		return new PullJavaFunction(object);
	}

	public Class<?> getClass(String className, Reader source, Iterable<Library> dependencies) throws IOException {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		Map<String, JavaFileObject> output = new HashMap<String, JavaFileObject>();

		// A loader that searches our cache first.

		ClassLoader loader = new RAMClassLoader(output);

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		// Create a JavaFileManager which uses our DiagnosticCollector,
		// and creates a new RAMJavaFileObject for the class, and
		// registers it in our cache

		String sourceAsString = IOUtils.toString(source);

		StandardJavaFileManager sjfm = compiler.getStandardFileManager(diagnostics, Locale.getDefault(),
				Charset.defaultCharset());
		JavaFileManager jfm = new RAMFileManager(sjfm, output, loader, dependencies);

		// Create source file objects
		JavaSourceFromString src = new JavaSourceFromString(className, sourceAsString);

		CompilationTask task = compiler.getTask(null, jfm, diagnostics, null, null, Arrays.asList(src));
		if (!task.call()) {
			for (Diagnostic<? extends JavaFileObject> dm : diagnostics.getDiagnostics())
				System.err.println(dm);
			throw new RuntimeException("Compilation of source failed");
		}

		try {
			return loader.loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Improper code for getting runtime class", e);
		}
	}

	private static List<Library> getLibraries(String sourceAsString, LibraryFactory factory) {
		List<Library> dependentJars = new ArrayList<Library>();
		Matcher importMatcher = Pattern.compile("\\@ImportLibrary\\s*\\(\\s*([^\\)]+)\\s*\\)", Pattern.MULTILINE)
				.matcher(sourceAsString);
		while (importMatcher.find()) {
			String[] nameVersion = importMatcher.group(1).split("\\s*,\\s*");
			String name = null;
			String group = null;
			String version = "1";
			for (String nameOrVersion : nameVersion) {
				String[] keyValue = nameOrVersion.split("\\s*=\\s*");
				if (keyValue[0].equals("name")) {
					name = keyValue[1].substring(1, keyValue[1].length() - 1);
				} else if (keyValue[0].equals("group")) {
					group = keyValue[1].substring(1, keyValue[1].length() - 1);
				} else {
					version = keyValue[1].substring(1, keyValue[1].length() - 1);
				}
			}
			log.info("Found import for library " + name + "/" + group + " with version " + version);
			dependentJars.add(factory.getLibrary(group, name, version));
		}
		return dependentJars;
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
			InputStream stream = loader.getResourceAsStream(name.replaceAll("\\.", "/") + ".class");
			return stream;
		}

	}

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

	static final class RAMClassLoader extends ClassLoader {
		private final Map<String, JavaFileObject> output;

		RAMClassLoader(Map<String, JavaFileObject> output) {
			super(RAMClassLoader.class.getClassLoader());
			this.output = output;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			JavaFileObject jfo = output.get(name);
			System.out.println("Looking for " + name);
			if (jfo != null) {
				byte[] bytes = ((MemoryJavaFileObject) jfo).output.toByteArray();
				return defineClass(name, bytes, 0, bytes.length);
			}
			System.out.println("Looking in super classloader");
			return super.findClass(name);
		}
	}

	static class RAMFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

		private final Map<String, JavaFileObject> output;

		private final ClassLoader ldr;

		private Iterable<Library> dependencies;

		public RAMFileManager(StandardJavaFileManager sjfm, Map<String, JavaFileObject> output, ClassLoader ldr,
				Iterable<Library> dependencies) {
			super(sjfm);
			this.output = output;
			this.ldr = ldr;
			this.dependencies = dependencies;
		}

		public JavaFileObject getJavaFileForOutput(Location location, String name, Kind kind, FileObject sibling)
				throws IOException {
			JavaFileObject jfo = new MemoryJavaFileObject(name, kind);
			output.put(name, jfo);
			return jfo;
		}

		public ClassLoader getClassLoader(JavaFileManager.Location location) {
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
			else
				result = super.inferBinaryName(loc, jfo);

			return result;
		}

		@Override
		public Iterable<JavaFileObject> list(Location loc, String pkg, Set<Kind> kind, boolean recurse)
				throws IOException {

			Iterable<JavaFileObject> result = super.list(loc, pkg, kind, recurse);

			// Built-ins get loaded from this file manager
			if (loc == StandardLocation.CLASS_PATH && kind.contains(JavaFileObject.Kind.CLASS)) {
				ArrayList<JavaFileObject> temp = new ArrayList<JavaFileObject>(3);
				for (JavaFileObject jfo : result)
					temp.add(jfo);
				// First add all dependencies
				for (Library dependency : dependencies) {
					JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(dependency.getData()));
					try {
						JarEntry entry;
						while ((entry = jarIn.getNextJarEntry()) != null) {
							// System.out.println(entry.getName());
							if (entry.getName().startsWith(pkg.replaceAll("\\.", "/"))) {
								String rest = entry.getName().substring(pkg.length() + 1);
								if (rest.indexOf('/') < 0 && rest.indexOf('$') < 0 && rest.endsWith(".class")) {
									temp.add(new JarJavaFileObject(pkg + "." + rest.substring(0, rest.length() - 6),
											Kind.CLASS, ldr));
								}
							}
						}
					} finally {
						jarIn.close();
					}
				}

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
									temp.add(new JarJavaFileObject(pkg + "." + rest.substring(0, rest.length() - 6),
											Kind.CLASS, ldr));
								}
							}
						}

					}
				}
				result = temp;
			}
			return result;
		}
	}

	@Override
	public Transformation generateTransformation(GraphDatabaseService service, String qualifiedName, Reader reader,
			long version) throws IOException {
		TypeFactory factory = TypeFactory.getTypeFactory(service, true);

		LibraryFactory libraryFactory = LibraryFactory.getLibraryFactory(service, true);

		String code = IOUtils.toString(reader);

		List<Library> libraries = getLibraries(code, libraryFactory);

		Class<?> clazz = getClass(qualifiedName, new StringReader(code), libraries);
		if (!JavaFunction.class.isAssignableFrom(clazz)) {
			return null;
		}
		Transformation trans = new Transformation(service, qualifiedName);
		for (Library library : libraries) {
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
				Type type;
				java.lang.reflect.Type fieldType = field.getGenericType();
				InputType inputType;
				boolean varargs;
				if (factory.getType(getTypeName(fieldType)) != null) {
					type = TypeFactory.getTypeFactory(service).getType(getTypeName(fieldType));
					inputType = InputType.SERIAL;
					varargs = false;
				} else if (fieldType instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType) fieldType;
					if ((Iterable.class.isAssignableFrom((Class<?>) paramType.getRawType()) || Iterator.class
							.isAssignableFrom((Class<?>) paramType.getRawType()))
							&& factory.getType(getTypeName(paramType.getActualTypeArguments()[0])) != null) {
						type = factory.getType(getTypeName(paramType.getActualTypeArguments()[0]));
						inputType = InputType.AGGREGATE;
						varargs = false;
					} else {
						continue;
					}
				} else if (fieldType instanceof Class<?> && ((Class<?>) fieldType).isArray()) {
					// Varargs
					Class<?> componentType = ((Class<?>) fieldType).getComponentType();
					if (factory.getType(getTypeName(componentType)) != null) {
						type = factory.getType(getTypeName(componentType));
						inputType = InputType.SERIAL;
						varargs = true;
					} else {
						continue;
					}
				} else if (fieldType instanceof GenericArrayType) {
					ParameterizedType paramType = (ParameterizedType) ((GenericArrayType) fieldType)
							.getGenericComponentType();
					if (Iterator.class.isAssignableFrom((Class<?>) paramType.getRawType())
							&& factory.getType(getTypeName(paramType.getActualTypeArguments()[0])) != null) {
						type = factory.getType(getTypeName(paramType.getActualTypeArguments()[0]));
						inputType = InputType.AGGREGATE;
						varargs = true;
					} else {
						continue;
					}
				} else {
					continue;
				}
				// Run through the nested structure of the type and generate
				// input points for every level
				InputPoint inputPoint = new InputPoint(service, trans, field.getName(), type, inputType, true, varargs);
				generateInputPoints(service, trans, inputPoint, type);

			} else if (field.getAnnotation(OutputChannel.class) != null) {
				// An output
				Type type;
				java.lang.reflect.Type fieldType = field.getGenericType();
				if (factory.getType(getTypeName(fieldType)) != null) {
					type = factory.getType(getTypeName(fieldType));
				} else if (fieldType instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType) fieldType;
					if ((Iterable.class.isAssignableFrom((Class<?>) paramType.getRawType()) || OutputWriter.class
							.isAssignableFrom((Class<?>) paramType.getRawType()))
							&& factory.getType(getTypeName(paramType.getActualTypeArguments()[0])) != null) {
						type = factory.getType(getTypeName(paramType.getActualTypeArguments()[0]));
					} else {
						continue;
					}
				} else {
					continue;
				}
				OutputPoint outputPoint = new OutputPoint(service, trans, field.getName(), type);
				generateOutputPoints(service, trans, outputPoint, type);
			}
		}

		// Put code in
		trans.setCode(code);
		trans.setVersion(version);

		return trans;
	}

	private void generateInputPoints(GraphDatabaseService service, Transformation trans, TransformationField parent,
			Type type) {
		// Make input points for all the subfields
		for (org.webseer.model.meta.Field field : type.getFields()) {
			TransformationField subField = new TransformationField(service, parent, field);
			generateInputPoints(service, trans, subField, field.getType());
		}
	}

	private void generateOutputPoints(GraphDatabaseService service, Transformation trans, TransformationField parent,
			Type type) {
		// Make input points for all the subfields
		for (org.webseer.model.meta.Field field : type.getFields()) {
			TransformationField subField = new TransformationField(service, parent, field);
			generateOutputPoints(service, trans, subField, field.getType());
		}
	}

	@Override
	public Type generateType(GraphDatabaseService service, String qualifiedName, Reader reader, long version)
			throws IOException {
		TypeFactory factory = TypeFactory.getTypeFactory(service);

		LibraryFactory libraryFactory = LibraryFactory.getLibraryFactory(service, true);

		String code = IOUtils.toString(reader);

		List<Library> libraries = getLibraries(code, libraryFactory);

		Class<?> clazz = getClass(qualifiedName, new StringReader(code), libraries);
		if (clazz.getAnnotation(org.webseer.type.Type.class) == null) {
			return null;
		}
		Type type = new Type(service, qualifiedName);

		// Read all the fields
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			java.lang.reflect.Type fieldType = field.getGenericType();
			if (factory.getType(getTypeName(fieldType)) != null) {
				Type typeObject = factory.getType(getTypeName(fieldType));
				type.addField(service, typeObject, field.getName(), false);
			} else if (fieldType instanceof Class<?> && ((Class<?>) fieldType).isArray()) {
				// Repeated
				Class<?> componentType = ((Class<?>) fieldType).getComponentType();
				if (factory.getType(getTypeName(componentType)) != null) {
					Type typeObject = factory.getType(getTypeName(componentType));
					type.addField(service, typeObject, field.getName(), true);
				}
			}

		}

		// Put code in
		type.setVersion(version);

		return type;
	}

	public static String getTypeName(java.lang.reflect.Type typeClazz) {
		if (PRIMITIVE_MAP.containsKey(typeClazz)) {
			return PRIMITIVE_MAP.get(typeClazz);
		}
		// Otherwise, use the full name
		if (typeClazz instanceof Class<?>) {
			return ((Class<?>) typeClazz).getName();
		}
		return null;
	}

}
