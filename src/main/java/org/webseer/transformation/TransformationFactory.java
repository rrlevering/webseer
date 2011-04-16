package org.webseer.transformation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.InputType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationField;
import org.webseer.model.meta.Type;
import org.webseer.type.TypeFactory;

public class TransformationFactory {

	private static Map<NeoService, TransformationFactory> SINGLETON = new HashMap<NeoService, TransformationFactory>();

	private final Node underlyingNode;

	public TransformationFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public void addTransformation(Transformation type) {
		if (getTransformation(type.getName()) != null) {
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

	public static TransformationFactory getTransformationFactory(NeoService service) {
		return getTransformationFactory(service, false);
	}

	public static TransformationFactory getTransformationFactory(NeoService service, boolean bootstrap) {
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

	public Transformation getTransformation(String id) {
		Transformation latest = null;
		Long version = null;
		for (Transformation type : getAllTransformations()) {
			if (type.getName().equals(id)) {
				if (latest == null || (version == null && type.getVersion() != null)
						|| (version != null && type.getVersion() != null && version < type.getVersion())) {
					latest = type;
					version = type.getVersion();
				}
			}
		}
		return latest;
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

	public Transformation getBucketTransformation(NeoService service) {
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

	private void bootstrapBuiltins(NeoService service) {
		Set<Transformation> blah = new HashSet<Transformation>();
		for (Transformation transformation : getAllTransformations()) {
			blah.add(transformation);
		}
		for (Transformation transformation : blah) {
			System.out.println("Removing " + transformation.getName());
			removeTransformation(transformation);
		}

		TypeFactory factory = TypeFactory.getTypeFactory(service);
		factory.bootstrapBuiltins(service);

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
				System.out.println("Removing " + transformation.getName());
				removeTransformation(transformation);
			}

			tran.success();
		} finally {
			tran.finish();
		}
	}

	private void recurBuiltins(NeoService service, File builtInDir, String packageName, Set<String> found) {
		for (File javaFile : builtInDir.listFiles()) {
			if (!javaFile.isDirectory()) {
				int sepPos = javaFile.getName().lastIndexOf('.');
				if (!javaFile.getName().substring(sepPos + 1).equals("java")) {
					continue;
				}
				String className = javaFile.getName().substring(0, sepPos);
				String qualifiedName = packageName + "." + className;
				Transformation trans = getTransformation(qualifiedName);

				found.add(qualifiedName);

				try {
					if (trans == null) {
						trans = createTransformation(service, qualifiedName, new FileReader(javaFile),
								javaFile.lastModified());
						if (trans != null) {
							addTransformation(trans);

							System.out.println("Added " + qualifiedName);
						}
					} else {
						long modified = javaFile.lastModified();
						if (trans.getVersion() == null || trans.getVersion() < modified) {
							Transformation newTrans = createTransformation(service, qualifiedName, new FileReader(
									javaFile), javaFile.lastModified());

							if (newTrans != null) {

								removeTransformation(trans);
								addTransformation(newTrans);

								System.out.println("Replaced " + qualifiedName);
							}
						} else {
							System.out.println("Found " + qualifiedName);
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

	private Transformation createTransformation(NeoService service, String qualifiedName, Reader reader, long version)
			throws IOException {
		String code = IOUtils.toString(reader);
		Class<?> clazz = JavaRuntimeFactory.getClass(qualifiedName, new StringReader(code));
		if (!JavaFunction.class.isAssignableFrom(clazz)) {
			return null;
		}
		Transformation trans = new Transformation(service, qualifiedName);

		TypeFactory factory = TypeFactory.getTypeFactory(service);

		// Get inputs and outputs
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			if (field.getAnnotation(InputChannel.class) != null) {
				// An input
				Type type;
				java.lang.reflect.Type fieldType = field.getGenericType();
				InputType inputType;
				boolean varargs;
				if (factory.getType(TypeFactory.getTypeName(fieldType)) != null) {
					type = TypeFactory.getTypeFactory(service).getType(TypeFactory.getTypeName(fieldType));
					inputType = InputType.SERIAL;
					varargs = false;
				} else if (fieldType instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType) fieldType;
					if ((Iterable.class.isAssignableFrom((Class<?>) paramType.getRawType()) || Iterator.class
							.isAssignableFrom((Class<?>) paramType.getRawType()))
							&& factory.getType(TypeFactory.getTypeName(paramType.getActualTypeArguments()[0])) != null) {
						type = factory.getType(TypeFactory.getTypeName(paramType.getActualTypeArguments()[0]));
						inputType = InputType.AGGREGATE;
						varargs = false;
					} else {
						continue;
					}
				} else if (fieldType instanceof Class<?> && ((Class<?>) fieldType).isArray()) {
					// Varargs
					Class<?> componentType = ((Class<?>) fieldType).getComponentType();
					if (factory.getType(TypeFactory.getTypeName(componentType)) != null) {
						type = factory.getType(TypeFactory.getTypeName(componentType));
						inputType = InputType.SERIAL;
						varargs = true;
					} else {
						continue;
					}
				} else if (fieldType instanceof GenericArrayType) {
					ParameterizedType paramType = (ParameterizedType) ((GenericArrayType) fieldType)
							.getGenericComponentType();
					if (Iterator.class.isAssignableFrom((Class<?>) paramType.getRawType())
							&& factory.getType(TypeFactory.getTypeName(paramType.getActualTypeArguments()[0])) != null) {
						type = factory.getType(TypeFactory.getTypeName(paramType.getActualTypeArguments()[0]));
						inputType = InputType.AGGREGATE;
						varargs = true;
					} else {
						continue;
					}
				} else {
					continue;
				}
				// Run through the nested structure of the type and generate input points for every level
				InputPoint inputPoint = new InputPoint(service, trans, field.getName(), type, inputType, true, varargs);
				generateInputPoints(service, trans, inputPoint, type);

			} else if (field.getAnnotation(OutputChannel.class) != null) {
				// An output
				Type type;
				java.lang.reflect.Type fieldType = field.getGenericType();
				if (factory.getType(TypeFactory.getTypeName(fieldType)) != null) {
					type = factory.getType(TypeFactory.getTypeName(fieldType));
				} else if (fieldType instanceof ParameterizedType) {
					ParameterizedType paramType = (ParameterizedType) fieldType;
					if ((Iterable.class.isAssignableFrom((Class<?>) paramType.getRawType()) || BucketOutputStream.class
							.isAssignableFrom((Class<?>) paramType.getRawType()))
							&& factory.getType(TypeFactory.getTypeName(paramType.getActualTypeArguments()[0])) != null) {
						type = factory.getType(TypeFactory.getTypeName(paramType.getActualTypeArguments()[0]));
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

	private void generateInputPoints(NeoService service, Transformation trans, TransformationField parent, Type type) {
		// Make input points for all the subfields
		for (org.webseer.model.meta.Field field : type.getFields()) {
			TransformationField subField = new TransformationField(service, parent, field);
			generateInputPoints(service, trans, subField, field.getType());
		}
	}

	private void generateOutputPoints(NeoService service, Transformation trans, TransformationField parent, Type type) {
		// Make input points for all the subfields
		for (org.webseer.model.meta.Field field : type.getFields()) {
			TransformationField subField = new TransformationField(service, parent, field);
			generateOutputPoints(service, trans, subField, field.getType());
		}
	}
}
