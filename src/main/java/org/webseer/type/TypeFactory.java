package org.webseer.type;

import java.util.HashMap;
import java.util.Map;

import name.levering.ryan.util.BiMap;
import name.levering.ryan.util.HashBiMap;

import org.apache.commons.lang.ArrayUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Type;

import com.google.protobuf.ByteString;

public class TypeFactory {

	private static final String[] PRIMITIVE_TYPES = new String[] { "bytes", "string", "bool", "sfixed64", "sfixed32",
			"sfixed64", "fixed64", "fixed32", "sint64", "sint32", "uint64", "uint32", "int64", "int32", "float",
			"double" };

	private static Map<GraphDatabaseService, TypeFactory> SINGLETON = new HashMap<GraphDatabaseService, TypeFactory>();

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
		casters.put("string", "int32", new CastFunction() {

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
		casters.put("int32", "string", new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Integer;
				return String.valueOf(object);
			}

		});
		casters.put("string", "int64", new CastFunction() {

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
		casters.put("int64", "string", new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Long;
				return String.valueOf(object);
			}

		});
		casters.put("string", "double", new CastFunction() {

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
		casters.put("double", "string", new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Double;
				return String.valueOf(object);
			}

		});
		casters.put("string", "float", new CastFunction() {

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
		casters.put("float", "string", new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Float;
				return String.valueOf(object);
			}

		});
		casters.put("string", "bool", new CastFunction() {

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
		casters.put("bool", "string", new CastFunction() {

			@Override
			public Object cast(Object object) throws CastException {
				assert object instanceof Boolean;
				return String.valueOf(object);
			}

		});
		casters.put("string", "bytes", new CastFunction() {

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
		casters.put("bytes", "string", new CastFunction() {

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
		if (!SINGLETON.containsKey(service)) {
			TypeFactory factory = Neo4JUtils.getSingleton(service, 
					TypeFactory.class);
			factory.bootstrapPrimitives(service);
			SINGLETON.put(service, factory);
		}
		TypeFactory factory = SINGLETON.get(service);
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

	private BiMap<String, String, CastFunction> casters = new HashBiMap<String, String, CastFunction>();

	public Object cast(Object object, Type sourceType, Type targetType) {
		if (sourceType.getName().equals(targetType.getName())) {
			return object;
		}
		CastFunction caster = casters.get(sourceType.getName(), targetType.getName());
		if (caster == null) {
			throw new RuntimeException("Can't cast type " + sourceType + " to " + targetType);
		}
		try {
			return caster.cast(object);
		} catch (CastException e) {
			return null;
		}
	}

	public void removeType(Type type) {
		Neo4JMetaUtils.getNode(type).getSingleRelationship(NeoRelationshipType.TYPE_FACTORY_TYPE, Direction.INCOMING)
				.delete();
	}

	public static interface CastFunction {

		public Object cast(Object object) throws CastException;

	}

	public static class CastException extends Exception {

		private static final long serialVersionUID = 1L;

	}

}
