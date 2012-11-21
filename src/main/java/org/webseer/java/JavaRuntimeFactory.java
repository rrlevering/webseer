package org.webseer.java;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.visitor.VoidVisitorAdapter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

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
import org.webseer.model.meta.LibraryResource;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.TransformationField;
import org.webseer.model.meta.Type;
import org.webseer.transformation.LanguageTransformationFactory;
import org.webseer.transformation.LibraryFactory;
import org.webseer.transformation.OutputWriter;
import org.webseer.transformation.PullRuntimeTransformation;
import org.webseer.type.LanguageTypeFactory;
import org.webseer.type.TypeFactory;

/**
 * The Java runtime factory is responsible for taking Java source from the database, compiling it, and returning a
 * runnable transformation.
 * 
 * @author ryan
 */
public class JavaRuntimeFactory implements LanguageTransformationFactory, LanguageTypeFactory {

	private static final Logger log = LoggerFactory.getLogger(JavaRuntimeFactory.class);

	private static Map<String, String> PRIMITIVE_MAP = new HashMap<String, String>();

	static {
		PRIMITIVE_MAP.put("java.lang.String", "string");
		PRIMITIVE_MAP.put("java.lang.Double", "double");
		PRIMITIVE_MAP.put("java.lang.Float", "float");
		PRIMITIVE_MAP.put("java.lang.Integer", "int32");
		PRIMITIVE_MAP.put("java.lang.Long", "int64");
		PRIMITIVE_MAP.put("java.lang.Boolean", "bool");
		PRIMITIVE_MAP.put("com.google.protobuf.ByteString", "bytes");
		PRIMITIVE_MAP.put("java.io.InputStream", "bytes");
		PRIMITIVE_MAP.put("java.io.OutputStream", "bytes");
	}

	public PullRuntimeTransformation generatePullTransformation(Transformation transformation)
			throws TransformationException {

		// Write out the Java source
		String className = transformation.getName();
		String code = transformation.getCode();

		// Get all the dependent types
		List<Type> types = new ArrayList<Type>();
		for (InputPoint input : transformation.getInputPoints()) {
			types.add(input.getType());
		}
		for (OutputPoint output : transformation.getOutputPoints()) {
			types.add(output.getType());
		}

		JavaFunction object;
		try {
			Class<?> clazz = getClass(className, new StringReader(code), types, transformation.getLibraries());
			object = (JavaFunction) clazz.newInstance();
		} catch (InstantiationException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (IllegalAccessException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (IOException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		}

		// Load and wrap the object with a java transformation wrapper
		return new ClassTransformation(object);
	}

	private Class<?> getClass(String className, Reader source, Iterable<Type> types, Iterable<Library> dependencies)
			throws IOException {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		Map<String, JavaFileObject> output = new HashMap<String, JavaFileObject>();

		// A loader that searches our cache first.

		RAMClassLoader loader = new RAMClassLoader(output);

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
	static class MemoryJarFileObject extends SimpleJavaFileObject {

		private Library library;
		private String name;

		protected MemoryJarFileObject(String name, Kind kind, Library library) {
			super(toURI(name, kind), kind);
			this.library = library;
			this.name = name;
		}

		@Override
		public InputStream openInputStream() throws IOException, IllegalStateException, UnsupportedOperationException {
			JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(library.getData()));
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
	 * This is used just to add on to the general classloader for loading the in-memory classes from the DB.
	 */
	static final class RAMClassLoader extends ClassLoader {
		private final Map<String, JavaFileObject> output;

		RAMClassLoader(Map<String, JavaFileObject> output) {
			super(RAMClassLoader.class.getClassLoader());
			this.output = output;
		}

		@Override
		protected URL findResource(String name) {
			return super.findResource(name);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				return findClass(name);
			} catch (ClassNotFoundException e) {
				return super.loadClass(name);
			}
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			JavaFileObject jfo = output.get(name);
			log.debug("Looking for " + name);
			if (jfo != null) {
				byte[] bytes;
				try {
					bytes = IOUtils.toByteArray(jfo.openInputStream());
				} catch (IOException e) {
					throw new ClassNotFoundException("Problem loading in memory class", e);
				}
				return defineClass(name, bytes, 0, bytes.length);
			}
			throw new ClassNotFoundException("Could not find class in memory loader");
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
			else if (loc == StandardLocation.CLASS_PATH && jfo instanceof MemoryJarFileObject)
				result = ((MemoryJarFileObject) jfo).name;
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
							if (entry.getName().startsWith(pkg.replaceAll("\\.", "/"))) {
								String rest = entry.getName().substring(pkg.length() + 1);
								if (rest.indexOf('/') < 0 && rest.indexOf('$') < 0 && rest.endsWith(".class")) {
									MemoryJarFileObject memoryJarFile = new MemoryJarFileObject(pkg + "."
											+ rest.substring(0, rest.length() - 6), Kind.CLASS, dependency);
									temp.add(memoryJarFile);
									output.put(pkg + "." + rest.substring(0, rest.length() - 6), memoryJarFile);
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

	/**
	 * Generates a transformation without any compilation. This essentially does the linking of the class file to the
	 * dependent classes.
	 */
	@Override
	public Collection<Transformation> generateTransformations(final GraphDatabaseService service, String qualifiedName,
			InputStream reader, final long version) throws IOException, TransformationException {
		final TypeFactory factory = TypeFactory.getTypeFactory(service, true);

		LibraryFactory libraryFactory = LibraryFactory.getLibraryFactory(service, true);

		final String code = IOUtils.toString(reader);

		CompilationUnit cu;
		try {
			// parse the file
			cu = JavaParser.parse(new ByteArrayInputStream(code.getBytes()));
		} catch (ParseException e) {
			throw new IOException("Problem parsing java transformation", e);
		} finally {
			reader.close();
		}

		// Go over the transformation and figure out:
		// 1) The transformations here with inputs and outputs
		// 2) The dependent libraries and types
		List<ImportDeclaration> importDecls = cu.getImports();

		final Set<Library> libraries = new HashSet<Library>();

		final String packageName = cu.getPackage().getName().toString();

		final Map<String, String> importLookup = new HashMap<String, String>();

		// Each import needs to either be in a library, a type, or the system library
		for (ImportDeclaration importDecl : importDecls) {
			NameExpr importName = importDecl.getName();
			String className = importName.toString();
			importLookup.put(importName.getName(), importName.toString());

			Type type = factory.getType(className);
			if (type != null) {
				System.out.println("Found type for " + type.getName());
			}

			LibraryResource resource = libraryFactory.getResource(className);
			if (resource != null) {
				System.out.println("Found library file for " + resource.getName());
				libraries.add(resource.getLibrary());
			}
		}

		final List<FieldDeclaration> inputFields = new ArrayList<FieldDeclaration>();
		final List<FieldDeclaration> outputFields = new ArrayList<FieldDeclaration>();
		final AnnotationExpr[] transform = new AnnotationExpr[1];

		final List<Transformation> transformations = new ArrayList<Transformation>();

		VoidVisitorAdapter<Object> visitor = new VoidVisitorAdapter<Object>() {

			@Override
			public void visit(ClassOrInterfaceDeclaration n, Object arg) {
				if (n.getAnnotations() != null) {
					for (AnnotationExpr annotation : n.getAnnotations()) {
						String annotationClass = resolveClass(importLookup, packageName, annotation.getName().getName());
						if (annotationClass.equals(FunctionDef.class.getName())) {
							// We know this is a class transformation
							System.out.println("Class transform = " + n.getName());
							transform[0] = annotation;
						}
					}
				}
				super.visit(n, arg);
			}

			public void visit(FieldDeclaration field, Object o) {
				if (field.getAnnotations() != null) {
					for (AnnotationExpr annotation : field.getAnnotations()) {
						String annotationClass = resolveClass(importLookup, packageName, annotation.getName().getName());
						if (annotationClass.equals(InputChannel.class.getName())) {
							// Add this to the input channel list for when we create the class transformation
							System.out.println("Input = " + field.getVariables());
							inputFields.add(field);
						} else if (annotationClass.equals(OutputChannel.class.getName())) {
							// Add this to the output channel list for when we create the class transformation
							System.out.println("Output = " + field.getVariables());
							outputFields.add(field);
						}
					}
				}
				super.visit(field, o);
			}

			public void visit(MethodDeclaration method, Object o) {
				if (method.getAnnotations() != null) {
					for (AnnotationExpr annotation : method.getAnnotations()) {
						String annotationClass = resolveClass(importLookup, packageName, annotation.getName().getName());
						if (annotationClass.equals(FunctionDef.class.getName())) {
							// We know this is a method transformation
							System.out.println("Method transform = " + method.getName());
							Transformation methodTransformation = new Transformation(service, method.getName());
							methodTransformation.setCode(code);
							methodTransformation.setVersion(version);

							addTransformationMetaInformation(methodTransformation, annotation);

							try {
								createOutputPoint(service, importLookup, packageName, factory, methodTransformation,
										method.getType(), "return");
								for (Parameter param : method.getParameters()) {
									createInputPoint(service, importLookup, packageName, factory, methodTransformation,
											param.getType(), param.getId().getName());
								}
							} catch (TransformationException e) {
								e.printStackTrace();
							}
							transformations.add(methodTransformation);
							for (Library library : libraries) {
								Neo4JMetaUtils.getNode(methodTransformation).createRelationshipTo(
										Neo4JMetaUtils.getNode(library), NeoRelationshipType.TRANSFORMATION_LIBRARY);
							}
						}
					}
				}
				super.visit(method, o);
			}

		};

		cu.accept(visitor, null);

		if (transform[0] != null) {
			// Generate a class transform
			Transformation classTransformation = new Transformation(service, qualifiedName);
			classTransformation.setCode(code);
			classTransformation.setVersion(version);
			addTransformationMetaInformation(classTransformation, transform[0]);
			for (FieldDeclaration inputField : inputFields) {
				createInputPoint(service, importLookup, packageName, factory, classTransformation,
						inputField.getType(), inputField.getVariables().get(0).getId().getName());
			}
			for (FieldDeclaration outputField : outputFields) {
				createOutputPoint(service, importLookup, packageName, factory, classTransformation,
						outputField.getType(), outputField.getVariables().get(0).getId().getName());
			}
			transformations.add(classTransformation);
			for (Library library : libraries) {
				Neo4JMetaUtils.getNode(classTransformation).createRelationshipTo(Neo4JMetaUtils.getNode(library),
						NeoRelationshipType.TRANSFORMATION_LIBRARY);
			}
		}

		return transformations;
	}

	private String resolveClass(Map<String, String> importLookup, String packageName, String name) {
		if (name.indexOf('.') >= 0) {
			return name;
		}
		String resolved = importLookup.get(name);
		if (resolved != null) {
			return resolved;
		}
		// java.lang is special cased
		if (name.equals("String") || name.equals("Boolean") || name.equals("Double") || name.equals("Integer")) {
			return "java.lang." + name;
		}
		return packageName + "." + name;
	}

	private static String transform(String resolved) {
		if (PRIMITIVE_MAP.containsKey(resolved)) {
			return PRIMITIVE_MAP.get(resolved);
		}
		return resolved;
	}

	private void addTransformationMetaInformation(Transformation methodTransformation, AnnotationExpr annotation) {
		if (annotation instanceof NormalAnnotationExpr) {
			List<MemberValuePair> nameValues = ((NormalAnnotationExpr) annotation).getPairs();
			for (MemberValuePair nameValue : nameValues) {
				if (nameValue.getName().equals("description")) {
					String description = ((StringLiteralExpr) nameValue.getValue()).getValue();
					methodTransformation.setDescription(description);
				} else if (nameValue.getName().equals("keywords")) {
					List<Expression> keywordExprs = ((ArrayInitializerExpr) nameValue.getValue()).getValues();
					String[] keywords = new String[keywordExprs.size()];
					for (int i = 0; i < keywords.length; i++) {
						keywords[i] = ((StringLiteralExpr) keywordExprs.get(i)).getValue();
					}
					methodTransformation.setKeyWords(keywords);
				}
			}
		}
	}

	private void createOutputPoint(GraphDatabaseService service, Map<String, String> importLookup, String packageName,
			TypeFactory types, Transformation trans, japa.parser.ast.type.Type parsedType, String fieldName)
			throws TransformationException {
		// An output
		Type type = getSimpleType(types, importLookup, packageName, parsedType);
		if (type == null) {
			if (parsedType instanceof ReferenceType) {
				japa.parser.ast.type.Type referencedType = ((ReferenceType) parsedType).getType();
				if (referencedType instanceof ClassOrInterfaceType) {
					String rawName = ((ClassOrInterfaceType) referencedType).getName();
					// FIXME...lots of crap here
					if (rawName.equals(Iterable.class.getSimpleName())
							|| rawName.equals(OutputWriter.class.getSimpleName())) {
						String genericName = ((ClassOrInterfaceType) ((ReferenceType) ((ClassOrInterfaceType) referencedType)
								.getTypeArgs().get(0)).getType()).getName();
						type = types.getType(transform(resolveClass(importLookup, packageName, genericName)));
					} else {
						throw new TransformationException("No type defined for " + fieldName);
					}
				} else {
					throw new IllegalStateException("Unrecognized type type");
				}
			} else {
				throw new IllegalStateException("Unrecognized type type");
			}
		}
		OutputPoint outputPoint = new OutputPoint(service, trans, fieldName, type);
		generateOutputPoints(service, trans, outputPoint, type);
	}

	private Type getSimpleType(TypeFactory types, Map<String, String> importLookup, String packageName,
			japa.parser.ast.type.Type parsedType) throws TransformationException {
		if (parsedType instanceof PrimitiveType) {
			String primitiveString;
			switch (((PrimitiveType) parsedType).getType()) {
			case Boolean:
				primitiveString = "bool";
				break;
			case Byte:
				primitiveString = "byte";
				break;
			case Char:
				primitiveString = "char";
				break;
			case Double:
				primitiveString = "double";
				break;
			case Float:
				primitiveString = "float";
				break;
			case Int:
				primitiveString = "int32";
				break;
			case Long:
				primitiveString = "int64";
				break;
			case Short:
				primitiveString = "int16";
				break;
			default:
				throw new IllegalStateException("Unrecognized primitive enum");
			}
			return types.getType(primitiveString);
		}
		if (parsedType instanceof ClassOrInterfaceType) {
			String rawName = ((ClassOrInterfaceType) parsedType).getName();
			String resolved = transform(resolveClass(importLookup, packageName, rawName));
			return types.getType(resolved);
		}
		if (parsedType instanceof ReferenceType) {
			if (((ReferenceType) parsedType).getArrayCount() > 0) {
				return null; // Don't handle arrays here
			}
			japa.parser.ast.type.Type referencedType = ((ReferenceType) parsedType).getType();
			if (referencedType instanceof ClassOrInterfaceType) {
				String rawName = ((ClassOrInterfaceType) referencedType).getName();
				String resolved = transform(resolveClass(importLookup, packageName, rawName));
				return types.getType(resolved);
			}
		}
		return null;
	}

	private void createInputPoint(GraphDatabaseService service, Map<String, String> importLookup, String packageName,
			TypeFactory types, Transformation trans, japa.parser.ast.type.Type parsedType, String fieldName)
			throws TransformationException {
		// An input
		Type type = getSimpleType(types, importLookup, packageName, parsedType);
		InputType inputType;
		boolean varargs;
		if (type != null) {
			inputType = InputType.SERIAL;
			varargs = false;
		} else {
			if (parsedType instanceof ReferenceType) {
				japa.parser.ast.type.Type referencedType = ((ReferenceType) parsedType).getType();
				if (referencedType instanceof ClassOrInterfaceType) {
					String rawName = ((ClassOrInterfaceType) referencedType).getName();
					// FIXME...lots of crap here
					if (rawName.equals(Iterator.class.getSimpleName())
							|| rawName.equals(Iterable.class.getSimpleName())) {
						String genericName = ((ClassOrInterfaceType) ((ReferenceType) ((ClassOrInterfaceType) referencedType)
								.getTypeArgs().get(0)).getType()).getName();
						type = types.getType(transform(resolveClass(importLookup, packageName, genericName)));
						inputType = InputType.AGGREGATE;
						varargs = false;
					} else {
						throw new TransformationException("No type defined for " + fieldName);
					}
				} else {
					throw new IllegalStateException("Unrecognized type type");
				}
			} else {
				throw new IllegalStateException("Unrecognized type type");
			}
		}
		// Run through the nested structure of the type and generate
		// input points for every level
		InputPoint inputPoint = new InputPoint(service, trans, fieldName, type, inputType, true, varargs);
		generateInputPoints(service, trans, inputPoint, type);
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
	public Collection<Type> generateTypes(GraphDatabaseService service, String qualifiedName, InputStream reader,
			long version) throws IOException, TransformationException {
		TypeFactory factory = TypeFactory.getTypeFactory(service);

		LibraryFactory libraryFactory = LibraryFactory.getLibraryFactory(service, true);

		String code = IOUtils.toString(reader);

		CompilationUnit cu;
		try {
			// parse the file
			cu = JavaParser.parse(new ByteArrayInputStream(code.getBytes()));
		} catch (ParseException e) {
			throw new IOException("Problem parsing java transformation", e);
		} finally {
			reader.close();
		}

		// Go over the transformation and figure out:
		// 1) The transformations here with inputs and outputs
		// 2) The dependent libraries and types
		List<ImportDeclaration> importDecls = cu.getImports();

		final Set<Library> libraries = new HashSet<Library>();

		final String packageName = cu.getPackage().getName().toString();

		final Map<String, String> importLookup = new HashMap<String, String>();

		// Each import needs to either be in a library, a type, or the system library
		for (ImportDeclaration importDecl : importDecls) {
			NameExpr importName = importDecl.getName();
			String className = importName.toString();
			importLookup.put(importName.getName(), importName.toString());

			Type type = factory.getType(className);
			if (type != null) {
				System.out.println("Found type for " + type.getName());
			}

			LibraryResource resource = libraryFactory.getResource(className);
			if (resource != null) {
				System.out.println("Found library file for " + resource.getName());
				libraries.add(resource.getLibrary());
			}
		}

		final List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
		final AnnotationExpr[] transform = new AnnotationExpr[1];

		VoidVisitorAdapter<Object> visitor = new VoidVisitorAdapter<Object>() {

			@Override
			public void visit(ClassOrInterfaceDeclaration n, Object arg) {
				if (n.getAnnotations() != null) {
					for (AnnotationExpr annotation : n.getAnnotations()) {
						String annotationClass = resolveClass(importLookup, packageName, annotation.getName().getName());
						if (annotationClass.equals(org.webseer.java.Type.class.getName())) {
							// We know this is a class transformation
							System.out.println("Class transform = " + n.getName());
							transform[0] = annotation;
						}
					}
				}
				super.visit(n, arg);
			}

			public void visit(FieldDeclaration field, Object o) {
				fields.add(field);
				super.visit(field, o);
			}

		};

		cu.accept(visitor, null);

		if (transform[0] == null) {
			return Collections.emptyList();
		}
		Type type = new Type(service, qualifiedName);

		// Read all the fields
		for (FieldDeclaration field : fields) {
			Type fieldType = getSimpleType(factory, importLookup, packageName, field.getType());
			if (fieldType == null) {
				// array
				if (field.getType() instanceof ReferenceType) {
					japa.parser.ast.type.Type referencedType = ((ReferenceType) field.getType()).getType();
					fieldType = getSimpleType(factory, importLookup, packageName, referencedType);
				}
				type.addField(service, fieldType, field.getVariables().get(0).getId().getName(), true);
			} else {
				type.addField(service, fieldType, field.getVariables().get(0).getId().getName(), false);

			}
		}

		// Put code in
		type.setVersion(version);

		return Collections.singleton(type);
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

	@Override
	public Library generateLibrary(GraphDatabaseService service, String packageName, String libraryName,
			String version, InputStream libraryData) throws IOException {
		log.info("Generating java library for {} in group {} with version {}", libraryName, packageName, version);
		byte[] jarBytes = IOUtils.toByteArray(libraryData);
		Library library = new Library(service, packageName, libraryName, version, jarBytes);
		JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(jarBytes));
		try {
			JarEntry entry;
			while ((entry = jarIn.getNextJarEntry()) != null) {
				if (entry.getName().endsWith(".class")) {
					byte[] classBuffer = IOUtils.toByteArray(jarIn);
					new LibraryResource(service, library, entry.getName().substring(0, entry.getName().length() - 6)
							.replace('/', '.'), classBuffer);
				}
			}
		} finally {
			jarIn.close();
		}
		return library;
	}
}
