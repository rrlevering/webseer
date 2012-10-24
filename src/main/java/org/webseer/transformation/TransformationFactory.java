package org.webseer.transformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.InputType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.type.TypeFactory;

public class TransformationFactory {

	private static final Logger log = LoggerFactory.getLogger(TransformationFactory.class);

	private static Map<GraphDatabaseService, TransformationFactory> SINGLETON = new HashMap<GraphDatabaseService, TransformationFactory>();

	private final Node underlyingNode;

	public TransformationFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public void addTransformation(Transformation type) {
		if (getLatestTransformationByName(type.getName()) != null) {
			throw new RuntimeException("Can't add a type with the same name");
		}
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(type),
				NeoRelationshipType.TRANSFORMATION_FACTORY_TRANSFORMATION);
	}

	public void removeTransformation(Transformation type) {
		Neo4JMetaUtils.getNode(type)
				.getSingleRelationship(NeoRelationshipType.TRANSFORMATION_FACTORY_TRANSFORMATION, Direction.INCOMING)
				.delete();
	}

	public static TransformationFactory getTransformationFactory(GraphDatabaseService service) {
		return getTransformationFactory(service, false);
	}

	public static TransformationFactory getTransformationFactory(GraphDatabaseService service, boolean bootstrap) {
		if (!SINGLETON.containsKey(service)) {
			TransformationFactory factory = Neo4JUtils.getSingleton(service,
					NeoRelationshipType.REFERENCE_TRANSFORMATION_FACTORY, TransformationFactory.class);
			SINGLETON.put(service, factory);
			if (bootstrap) {
				factory.bootstrapBuiltins(service);
			}
			return factory;
		}
		TransformationFactory factory = SINGLETON.get(service);
		// factory.bootstrapNative(service);
		return factory;
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public Transformation getLatestTransformationByName(String name) {
		Transformation latest = null;
		Long version = null;
		for (Transformation type : getAllTransformations()) {
			if (type.getName().equals(name)) {
				if (latest == null || (version == null && type.getVersion() != null)
						|| (version != null && type.getVersion() != null && version < type.getVersion())) {
					latest = type;
					version = type.getVersion();
				}
			}
		}
		return latest;
	}

	public Transformation getTransformation(long id) {
		for (Transformation type : getAllTransformations()) {
			if (Neo4JMetaUtils.getNode(type).getId() == id) {
				return type;
			}
		}
		return null;
	}

	public Iterable<Transformation> getAllTransformations() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.TRANSFORMATION_FACTORY_TRANSFORMATION,
				Transformation.class);
	}

	public String toString() {
		return "TransformationFactory";
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TransformationFactory)) {
			return false;
		}
		return ((TransformationFactory) o).getUnderlyingNode().equals(underlyingNode);
	}

	private final static String BUILTIN_DIR = "org/webseer";

	public Transformation getBucketTransformation(GraphDatabaseService service) {
		if (Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATION_FACTORY_BUCKET,
				Transformation.class) == null) {
			Transformation trans = new Transformation(service, "Workspace Bucket");
			new InputPoint(service, trans, "itemToAdd", TypeFactory.getTypeFactory(service).getType("string"),
					InputType.SERIAL, true, false);
			new OutputPoint(service, trans, "itemToOutput", TypeFactory.getTypeFactory(service).getType("string"));
			underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(trans),
					NeoRelationshipType.TRANSFORMATION_FACTORY_BUCKET);
		}
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATION_FACTORY_BUCKET,
				Transformation.class);
	}

	private void bootstrapBuiltins(GraphDatabaseService service) {
		Set<Transformation> blah = new HashSet<Transformation>();
		for (Transformation transformation : getAllTransformations()) {
			blah.add(transformation);
		}
		for (Transformation transformation : blah) {
			log.info("Removing transformation: " + transformation.getName());
			removeTransformation(transformation);
		}

		// Add/update all the builtin webseer transformations
		Transaction tran = service.beginTx();
		try {
			URL directory = getClass().getClassLoader().getResource(BUILTIN_DIR);
			File builtInDir;
			try {
				builtInDir = new File(directory.toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return;
			}
			Set<String> found = new HashSet<String>();
			recurBuiltins(service, builtInDir, "org.webseer", found);

			// Remove all the ones we didn't find
			Set<Transformation> toRemove = new HashSet<Transformation>();
			for (Transformation transformation : getAllTransformations()) {
				if (transformation.getPackage().startsWith("org.webseer") && !found.contains(transformation.getName())) {
					// Remove
					toRemove.add(transformation);
				}
			}
			for (Transformation transformation : toRemove) {
				log.info("Removing built-in transformation: " + transformation.getName());
				removeTransformation(transformation);
			}

			tran.success();
		} finally {
			tran.finish();
		}
	}

	private void recurBuiltins(GraphDatabaseService service, File builtInDir, String packageName, Set<String> found) {
		for (File javaFile : builtInDir.listFiles()) {
			if (!javaFile.isDirectory()) {
				int sepPos = javaFile.getName().lastIndexOf('.');
				if (!javaFile.getName().substring(sepPos + 1).equals("java")) {
					continue;
				}
				String className = javaFile.getName().substring(0, sepPos);
				String qualifiedName = packageName + "." + className;
				Transformation trans = getLatestTransformationByName(qualifiedName);

				found.add(qualifiedName);

				try {
					if (trans == null) {
						trans = createTransformation(service, qualifiedName, new FileReader(javaFile),
								javaFile.lastModified());
						if (trans != null) {
							addTransformation(trans);

							log.info("Added built-in transformation: " + qualifiedName);
						}
					} else {
						long modified = javaFile.lastModified();
						if (trans.getVersion() == null || trans.getVersion() < modified) {
							Transformation newTrans = createTransformation(service, qualifiedName, new FileReader(
									javaFile), javaFile.lastModified());

							if (newTrans != null) {

								removeTransformation(trans);
								addTransformation(newTrans);

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
			} else {
				recurBuiltins(service, javaFile, packageName.isEmpty() ? javaFile.getName()
						: (packageName + "." + javaFile.getName()), found);
			}
		}
	}

	private Transformation createTransformation(GraphDatabaseService service, String qualifiedName, Reader reader,
			long version) throws IOException {
		return LanguageFactory.getInstance().generateTransformation("Java", service, qualifiedName, reader, version);
	}

}
