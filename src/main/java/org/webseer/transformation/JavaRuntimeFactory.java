package org.webseer.transformation;

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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

import name.levering.ryan.util.BiMap;
import name.levering.ryan.util.HashBiMap;

import org.apache.commons.io.IOUtils;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.runtime.RuntimeConfiguration;
import org.webseer.model.runtime.RuntimeTransformationNode;

/**
 * The Java runtime factory is responsible for taking Java source from the database, compiling it, and returning a
 * runnable transformation.
 * 
 * @author ryan
 */
public class JavaRuntimeFactory implements RuntimeFactory {

	public static final String COMPILE_DIRECTORY = "build/java";

	public static final String RUNTIME_DIRECTORY = "runtime/java";

	static BiMap<String, Long, File> registry = new HashBiMap<String, Long, File>();

	static {
		// Get the jar repository FIXME
		File jarRepository = new File("/workspaces/webseer/webseer/WebContent/WEB-INF/lib");

		// Load the builtin jar registry
		InputStream registryStream = JavaRuntimeFactory.class.getResourceAsStream("/jar-registry");
		@SuppressWarnings("unchecked")
		List<String> lines;
		try {
			lines = IOUtils.readLines(registryStream);
			for (String line : lines) {
				String[] entry = line.split(",");
				String name = entry[0];
				long version = Long.parseLong(entry[1]);
				File file = new File(jarRepository, entry[2]);
				registry.put(name, version, file);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public PullRuntimeTransformation generatePullTransformation(RuntimeConfiguration config,
			RuntimeTransformationNode runtime) throws TransformationException {

		Transformation transformation = runtime.getTransformationNode().getTransformation();

		// Write out the Java source
		String className = transformation.getName();
		String code = transformation.getCode();

		JavaFunction object;
		try {
			Class<?> clazz = getClass(className, new StringReader(code));
			object = (JavaFunction) clazz.newInstance();
		} catch (InstantiationException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (IllegalAccessException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		} catch (IOException e) {
			throw new TransformationException("Unable to instantiate user code", e);
		}

		// Load and wrap the object with a java transformation wrapper
		return new PullJavaFunction(config, runtime, object);
	}

	public static Class<?> getClass(String className, Reader source) throws IOException {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		Map<String, JavaFileObject> output = new HashMap<String, JavaFileObject>();

		// A loader that searches our cache first.

		ClassLoader loader = new RAMClassLoader(output);

		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		// Create a JavaFileManager which uses our DiagnosticCollector,
		// and creates a new RAMJavaFileObject for the class, and
		// registers it in our cache

		String sourceAsString = IOUtils.toString(source);

		// Rip off whatever libraries are necessary to compile this class
		List<File> dependencies = getLibraries(sourceAsString);

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

	private static List<File> getLibraries(String sourceAsString) {
		List<File> dependentJars = new ArrayList<File>();
		Matcher importMatcher = Pattern.compile("\\@Import\\s*\\(\\s*(.+)\\s*\\)", Pattern.MULTILINE).matcher(
				sourceAsString);
		while (importMatcher.find()) {
			String[] nameVersion = importMatcher.group(1).split("\\s*,\\s*");
			String name = null;
			long version = 1L;
			for (String nameOrVersion : nameVersion) {
				String[] keyValue = nameOrVersion.split("\\s*=\\s*");
				if (keyValue[0].equals("name")) {
					name = keyValue[1].substring(1, keyValue[1].length() - 1);
				} else {
					version = Long.parseLong(keyValue[1]);
				}
			}
			dependentJars.add(registry.get(name, version));
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
			if (jfo != null) {
				byte[] bytes = ((MemoryJavaFileObject) jfo).output.toByteArray();
				return defineClass(name, bytes, 0, bytes.length);
			}
			return super.findClass(name);
		}
	}

	static class RAMFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

		private final Map<String, JavaFileObject> output;

		private final ClassLoader ldr;

		private List<File> dependencies;

		public RAMFileManager(StandardJavaFileManager sjfm, Map<String, JavaFileObject> output, ClassLoader ldr,
				List<File> dependencies) {
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
				for (File dependency : dependencies) {
					JarFile jarFile = new JarFile(dependency);
					Enumeration<JarEntry> iterator = jarFile.entries();
					while (iterator.hasMoreElements()) {
						JarEntry entry = iterator.nextElement();
						System.out.println(entry.getName());
						if (entry.getName().startsWith(pkg.replaceAll("\\.", "/"))) {
							String rest = entry.getName().substring(pkg.length() + 1);
							if (rest.indexOf('/') < 0 && rest.indexOf('$') < 0 && rest.endsWith(".class")) {
								temp.add(new JarJavaFileObject(pkg + "." + rest.substring(0, rest.length() - 6),
										Kind.CLASS, ldr));
							}
						}
					}
				}

				// We need to find the class files by looking up the package as a directory
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

}
