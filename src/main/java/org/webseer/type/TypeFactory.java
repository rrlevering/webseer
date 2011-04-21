package org.webseer.type;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import name.levering.ryan.util.BiMap;
import name.levering.ryan.util.HashBiMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Type;
import org.webseer.transformation.BucketOutputStream;
import org.webseer.transformation.JavaRuntimeFactory;

import com.google.protobuf.ByteString;

public class TypeFactory {

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
		PRIMITIVE_MAP.put(BucketOutputStream.class, "bytes");
	}

	private static final String[] PRIMITIVE_TYPES = new String[] { "bytes", "string", "bool", "sfixed64", "sfixed32",
			"sfixed64", "fixed64", "fixed32", "sint64", "sint32", "uint64", "uint32", "int64", "int32", "float",
			"double" };

	private final Node underlyingNode;

	public TypeFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	private void bootstrapPrimitives(GraphDatabaseService service) {
		for (String primitive : PRIMITIVE_TYPES) {
			if (getType(primitive) == null) {
				addType(new Type(service, primitive));
			}
		}

		// Number/string casts
		casters.put(getType("string"), getType("int32"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof String;
				try {
					return Integer.valueOf((String) object);
				} catch (NumberFormatException e) {
					throw new CastException();
				}
			}

		});
		casters.put(getType("int32"), getType("string"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Integer;
				return String.valueOf(object);
			}

		});
		casters.put(getType("string"), getType("int64"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof String;
				try {
					return Long.valueOf((String) object);
				} catch (NumberFormatException e) {
					throw new CastException();
				}
			}

		});
		casters.put(getType("int64"), getType("string"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Long;
				return String.valueOf(object);
			}

		});
		casters.put(getType("string"), getType("double"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof String;
				try {
					return Double.valueOf((String) object);
				} catch (NumberFormatException e) {
					throw new CastException();
				}
			}

		});
		casters.put(getType("double"), getType("string"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Double;
				return String.valueOf(object);
			}

		});
		casters.put(getType("string"), getType("float"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof String;
				try {
					return Float.valueOf((String) object);
				} catch (NumberFormatException e) {
					throw new CastException();
				}
			}

		});
		casters.put(getType("float"), getType("string"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Float;
				return String.valueOf(object);
			}

		});
		casters.put(getType("string"), getType("bool"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof String;
				try {
					return Boolean.valueOf((String) object);
				} catch (NumberFormatException e) {
					throw new CastException();
				}
			}

		});
		casters.put(getType("bool"), getType("string"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Boolean;
				return String.valueOf(object);
			}

		});
		casters.put(getType("string"), getType("bytes"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof String;
				try {
					return ByteString.copyFrom(((String) object).getBytes());
				} catch (NumberFormatException e) {
					throw new CastException();
				}
			}

		});
		casters.put(getType("bytes"), getType("string"), new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof ByteString;
				return ((ByteString) object).toStringUtf8();
			}

		});
	}

	public static boolean isPrimitive(String type) {
		return ArrayUtils.contains(PRIMITIVE_TYPES, type);
	}

	public void addType(Type type) {
		if (getType(type.getName()) != null) {
			throw new RuntimeException("Can't add a type with the same name");
		}
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(type), NeoRelationshipType.TYPE_FACTORY_TYPE);
	}

	public static TypeFactory getTypeFactory(GraphDatabaseService service) {
		return getTypeFactory(service, false);
	}

	public static TypeFactory getTypeFactory(GraphDatabaseService service, boolean bootstrap) {
		TypeFactory factory = Neo4JUtils.getSingleton(service, NeoRelationshipType.REFERENCE_TYPE_FACTORY,
				TypeFactory.class);
		factory.bootstrapPrimitives(service);
		if (bootstrap) {
			factory.bootstrapBuiltins(service);
		}
		return factory;
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public Type getType(String name) {
		for (Type type : getAllTypes()) {
			if (type.getName().equals(name)) {
				return type;
			}
		}
		return null;
	}

	public Iterable<Type> getAllTypes() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.TYPE_FACTORY_TYPE, Type.class);
	}

	public String toString() {
		return "TypeFactory";
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TypeFactory)) {
			return false;
		}
		return ((TypeFactory) o).getUnderlyingNode().equals(underlyingNode);
	}

	private BiMap<Type, Type, CastFunction> casters = new HashBiMap<Type, Type, CastFunction>();

	public Object cast(Object object, Type sourceType, Type targetType) {
		if (sourceType.equals(targetType)) {
			return object;
		}
		CastFunction caster = casters.get(sourceType, targetType);
		if (caster == null) {
			throw new RuntimeException("Can't cast type " + sourceType + " to " + targetType);
		}
		try {
			return caster.cast(object);
		} catch (CastException e) {
			return null;
		}
	}

	private final static String BUILTIN_DIR = "org/webseer";

	public void bootstrapBuiltins(GraphDatabaseService service) {
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
			Set<Type> toRemove = new HashSet<Type>();
			for (Type type : getAllTypes()) {
				if (type.getPackage().startsWith("org.webseer") && !found.contains(type.getName())) {
					// Remove
					toRemove.add(type);
				}
			}
			for (Type type : toRemove) {
				System.out.println("Removing " + type.getName());
				removeType(type);
			}

			tran.success();
		} finally {
			tran.finish();
		}
	}

	public void removeType(Type type) {
		Neo4JMetaUtils.getNode(type).getSingleRelationship(NeoRelationshipType.TYPE_FACTORY_TYPE, Direction.INCOMING)
				.delete();
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

	private void recurBuiltins(GraphDatabaseService service, File builtInDir, String packageName, Set<String> found) {
		for (File javaFile : builtInDir.listFiles()) {
			if (!javaFile.isDirectory()) {
				int sepPos = javaFile.getName().lastIndexOf('.');
				if (!javaFile.getName().substring(sepPos + 1).equals("java")) {
					continue;
				}
				String className = javaFile.getName().substring(0, sepPos);
				String qualifiedName = packageName + "." + className;
				Type type = getType(qualifiedName);

				found.add(qualifiedName);

				try {
					if (type == null) {
						type = createType(service, qualifiedName, new FileReader(javaFile), javaFile.lastModified());
						if (type != null) {
							addType(type);

							System.out.println("Added " + qualifiedName);
						}
					} else {
						long modified = javaFile.lastModified();
						if (type.getVersion() == null || type.getVersion() < modified) {
							Type newType = createType(service, qualifiedName, new FileReader(javaFile),
									javaFile.lastModified());

							if (newType != null) {

								removeType(type);
								addType(newType);

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

	private Type createType(GraphDatabaseService service, String qualifiedName, Reader reader, long version)
			throws IOException {
		String code = IOUtils.toString(reader);
		Class<?> clazz = JavaRuntimeFactory.getClass(qualifiedName, new StringReader(code));
		if (clazz.getAnnotation(org.webseer.type.Type.class) == null) {
			return null;
		}
		Type type = new Type(service, qualifiedName);

		// Read all the fields
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			java.lang.reflect.Type fieldType = field.getGenericType();
			if (getType(getTypeName(fieldType)) != null) {
				Type typeObject = getType(getTypeName(fieldType));
				type.addField(service, typeObject, field.getName(), false);
			} else if (fieldType instanceof Class<?> && ((Class<?>) fieldType).isArray()) {
				// Repeated
				Class<?> componentType = ((Class<?>) fieldType).getComponentType();
				if (getType(getTypeName(componentType)) != null) {
					Type typeObject = getType(getTypeName(componentType));
					type.addField(service, typeObject, field.getName(), true);
				}
			}

		}

		// Put code in
		type.setVersion(version);

		return type;
	}

	public static interface CastFunction {

		public Object cast(Object object) throws CastException;

	}

	public static class CastException extends Exception {

		private static final long serialVersionUID = 1L;

	}

}
