package org.webseer.transformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.UserType;
import org.webseer.type.TypeFactory;

/**
 * The bootstrapper searches certain directories for file types that can be registered with webseer.
 */
public class Bootstrapper {

	private static final Logger log = LoggerFactory.getLogger(TransformationFactory.class);

	private static boolean bootstrapped = false;

	private final static String BOOTSTRAP_FILE = "bootstrap-marker";

	public synchronized static void bootstrapBuiltins(GraphDatabaseService service) {
		if (bootstrapped) {
			return;
		}
		TransformationFactory factory = TransformationFactory.getTransformationFactory(service);

		TypeFactory types = TypeFactory.getTypeFactory(service);

		LibraryFactory libraries = LibraryFactory.getLibraryFactory(service);

		Set<Transformation> blah = new HashSet<Transformation>();
		for (Transformation transformation : factory.getAllTransformations()) {
			blah.add(transformation);
		}
		for (Transformation transformation : blah) {
			log.info("Removing transformation: " + transformation.getName());
			factory.removeTransformation(transformation);
		}

		HashSet<Library> librariesToRemove = new HashSet<Library>();
		for (Library library : libraries.getAllLibraries()) {
			librariesToRemove.add(library);
		}
		for (Library library : librariesToRemove) {
			log.info("Removing library: " + library.getGroup() + "/" + library.getName());
			libraries.removeLibrary(library);
		}

		// Add/update all the builtin webseer transformations
		Transaction tran = service.beginTx();
		try {
			URL directory = Bootstrapper.class.getClassLoader().getResource(BOOTSTRAP_FILE);
			File builtInDir;
			try {
				builtInDir = new File(directory.toURI()).getParentFile();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return;
			}
			Set<String> foundTransformations = new HashSet<String>();
			Set<String> foundTypes = new HashSet<String>();

			//TODO(rrlevering): Better lookup system so we don't have to do this fragile ordering
			recurLibraries(libraries, types, service, builtInDir, "");
			recurTypes(factory, types, service, builtInDir, "", foundTypes);
			recurTransformations(factory, types, service, builtInDir, "", foundTransformations);

			// Remove all the ones we didn't find...this is temporary until types get more predictable
			Set<UserType> typesToRemove = new HashSet<UserType>();
			for (UserType type : types.getAllTypes()) {
				if (!foundTypes.contains(type.getName())) {
					// Remove
					typesToRemove.add(type);
				}
			}
			for (UserType type : typesToRemove) {
				if (!type.isPrimitive()) {
					log.info("Removing type: " + type.getName());
					types.removeType(type);
				}
			}

			// Remove all the ones we didn't find
			Set<Transformation> toRemove = new HashSet<Transformation>();
			for (Transformation transformation : factory.getAllTransformations()) {
				if (!foundTransformations.contains(transformation.getName())) {
					// Remove
					toRemove.add(transformation);
				}
			}
			for (Transformation transformation : toRemove) {
				log.info("Removing built-in transformation: " + transformation.getName());
				factory.removeTransformation(transformation);
			}

			tran.success();
		} finally {
			tran.finish();
		}
	}

	private static void recurLibraries(LibraryFactory libraries, TypeFactory types,
			GraphDatabaseService service, File builtInDir, String packageName) {
		for (File file : builtInDir.listFiles()) {
			if (!file.isDirectory()) {
				int sepPos = file.getName().lastIndexOf('.');
				String extension = file.getName().substring(sepPos + 1);

				LanguageFactory languages = LanguageFactory.getInstance();

				String language = languages.getLanguageForLibraryExtension(extension);
				if (language != null) {
					addLibrary(libraries, service, packageName, file, sepPos, language);
				}

			} else {
				recurLibraries(libraries, types, service, file, packageName.isEmpty() ? file.getName()
						: (packageName + "." + file.getName()));
			}
		}
	}

	private static void recurTypes(TransformationFactory transformations, TypeFactory types,
			GraphDatabaseService service, File builtInDir, String packageName,
			Set<String> foundTypes) {
		for (File file : builtInDir.listFiles()) {
			if (!file.isDirectory()) {
				int sepPos = file.getName().lastIndexOf('.');
				String extension = file.getName().substring(sepPos + 1);

				LanguageFactory languages = LanguageFactory.getInstance();

				String language = languages.getLanguageForTypeExtension(extension);
				if (language != null) {
					addType(types, service, packageName, foundTypes, file, sepPos, language);
				}

			} else {
				recurTypes(transformations, types, service, file, packageName.isEmpty() ? file.getName()
						: (packageName + "." + file.getName()), foundTypes);
			}
		}
	}

	private static void recurTransformations(TransformationFactory transformations, TypeFactory types,
			GraphDatabaseService service, File builtInDir, String packageName, Set<String> foundTransformations) {
		for (File file : builtInDir.listFiles()) {
			if (!file.isDirectory()) {
				int sepPos = file.getName().lastIndexOf('.');
				String extension = file.getName().substring(sepPos + 1);

				LanguageFactory languages = LanguageFactory.getInstance();

				String language = languages.getLanguageForTransformationExtension(extension);
				if (language != null) {
					addTransformation(transformations, service, packageName, foundTransformations, file, sepPos,
							language);
				}
			} else {
				recurTransformations(transformations, types, service, file, packageName.isEmpty() ? file.getName()
						: (packageName + "." + file.getName()), foundTransformations);
			}
		}
	}

	private static void addLibrary(LibraryFactory libraries, GraphDatabaseService service,
			String packageName, File file, int sepPos, String language) {
		String group = file.getParentFile().getName();
		String nameAndVersion = file.getName().substring(0, sepPos);
		String name = nameAndVersion.substring(0, nameAndVersion.lastIndexOf('-'));
		String version = nameAndVersion.substring(nameAndVersion.lastIndexOf('-') + 1);

		try {
			Library library = LanguageFactory.getInstance().generateLibrary(language, service, group, name, new FileInputStream(file),
					version);
			libraries.addLibrary(library);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addType(TypeFactory types, GraphDatabaseService service, String packageName,
			Set<String> foundTypes, File file, int sepPos, String language) {
		String className = file.getName().substring(0, sepPos);
		String qualifiedName = packageName + "." + className;
		UserType type = types.getType(qualifiedName);

		foundTypes.add(qualifiedName);

		try {
			if (type == null) {
				// FIXME This fails now because it's trying to compile non types without libraries
				type = createType(service, qualifiedName, new FileReader(file), file.lastModified(), language);
				if (type != null) {
					types.addType(type);

					log.info("Added type: " + qualifiedName);
				}
			} else {
				long modified = file.lastModified();
				if (type.getVersion() == null || type.getVersion() < modified) {
					UserType newType = createType(service, qualifiedName, new FileReader(file), file.lastModified(),
							language);

					if (newType != null) {

						types.removeType(type);
						types.addType(newType);

						log.info("Replaced type: " + qualifiedName);
					}
				} else {
					log.info("Found type: " + qualifiedName);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addTransformation(TransformationFactory transformations, GraphDatabaseService service,
			String packageName, Set<String> foundTransformations, File file, int sepPos, String language) {
		String className = file.getName().substring(0, sepPos);
		String qualifiedName = packageName + "." + className;
		foundTransformations.add(qualifiedName);
		Transformation trans = transformations.getLatestTransformationByName(qualifiedName);

		try {
			if (trans == null) {
				trans = createTransformation(service, qualifiedName, new FileReader(file), file.lastModified(),
						language);
				if (trans != null) {
					transformations.addTransformation(trans);

					log.info("Added built-in transformation: " + qualifiedName);
				}
			} else {
				long modified = file.lastModified();
				if (trans.getVersion() == null || trans.getVersion() < modified) {
					Transformation newTrans = createTransformation(service, qualifiedName, new FileReader(file),
							file.lastModified(), language);

					if (newTrans != null) {

						transformations.removeTransformation(trans);
						transformations.addTransformation(newTrans);

						log.info("Replaced built-in transformation: " + qualifiedName);
					}
				} else {
					log.info("Found built-in transformation: " + qualifiedName);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Transformation createTransformation(GraphDatabaseService service, String qualifiedName,
			Reader reader, long version, String language) throws IOException {
		return LanguageFactory.getInstance().generateTransformation(language, service, qualifiedName, reader, version);
	}

	private static UserType createType(GraphDatabaseService service, String qualifiedName, Reader reader, long version,
			String language) throws IOException {
		return LanguageFactory.getInstance().generateType(language, service, qualifiedName, reader, version);
	}

}
