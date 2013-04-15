package org.webseer.model.meta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import name.levering.ryan.util.Pair;

import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.Neo4JUtils.NodeReader;
import org.webseer.model.NeoRelationshipType;
import org.webseer.streams.io.BucketIO;
import org.webseer.streams.io.bucket.RandomAccessBucketIO;
import org.webseer.streams.model.trace.Bucket;
import org.webseer.streams.model.trace.HasValue;
import org.webseer.type.TypeFactory;

import com.google.protobuf.ByteString;

public class Type {

	public static BucketIO reader = new RandomAccessBucketIO();

	private final static String NAME = "name";

	private final Node underlyingNode;

	public Type(GraphDatabaseService service, String string) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.setProperty(NAME, string);
	}

	public Type(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public void addField(GraphDatabaseService service, org.webseer.model.meta.Field field) {
		Neo4JUtils.addToList(service, underlyingNode, field.underlyingNode, NeoRelationshipType.TYPE_FIRST_FIELD,
				NeoRelationshipType.TYPE_LAST_FIELD, NeoRelationshipType.TYPE_FIELD);
	}

	public Iterable<org.webseer.model.meta.Field> getFields() {
		return Neo4JUtils.getListIterable(underlyingNode, NeoRelationshipType.TYPE_FIRST_FIELD,
				NeoRelationshipType.TYPE_FIELD, new NodeReader<org.webseer.model.meta.Field>() {

					@Override
					public org.webseer.model.meta.Field convertNode(Node node) {
						return Neo4JUtils.getInstance(node, org.webseer.model.meta.Field.class);
					}

				});
	}

	Node getUnderlyingNode() {
		return underlyingNode;
	}

	public String getName() {
		return (String) underlyingNode.getProperty(NAME);
	}

	public String getSimpleName() {
		String fullName = getName();
		return fullName.substring(fullName.lastIndexOf('.') + 1);
	}

	public String getPackage() {
		String fullName = getName();
		if (fullName.indexOf('.') < 0) {
			return "";
		}
		return fullName.substring(0, fullName.lastIndexOf('.'));
	}

	public boolean isPrimitive() {
		return TypeFactory.isPrimitive(getName());
	}

	public String getFieldName(Type type) {
		return (String) Neo4JUtils.getListItemProperty(underlyingNode, NeoRelationshipType.TYPE_FIRST_FIELD,
				NeoRelationshipType.TYPE_FIELD, type.underlyingNode, "fieldName");
	}

	/**
	 * Gets an input stream that can write raw data. This is used particularly for writing very large pieces of data
	 * that you can't easily hold in memory.
	 */
	public InputStream getInputStream(HasValue item) {
		if (getName().equals("bytes") && item.getOffset() != null) {
			long bucketId = item.getBucketId();
			Long offset = item.getOffset();

			return reader.getInputStream(bucketId, offset);
		}
		return null;
	}

	public OutputStream getOutputStream(HasValue item) {
		if (getName().equals("bytes")) {
			long bucketId = item.getBucketId();
			Pair<Long, OutputStream> output = reader.getOutputStream(bucketId, item.getOffset());
			item.setOffset(output.getFirst());
			return output.getSecond();
		}
		return null;
	}
	
	public String toString(Object value) {
		TypeFactory typeFactory = TypeFactory.getTypeFactory(underlyingNode.getGraphDatabase());
		return (String) typeFactory.cast(value, this, typeFactory.getType("string"));
	}
	
	public Object fromString(String value) {
		TypeFactory typeFactory = TypeFactory.getTypeFactory(underlyingNode.getGraphDatabase());
		return typeFactory.cast(value, typeFactory.getType("string"), this);
	}

	public Object getValue(HasValue item) {
		// If it's primitive, rip it off the node
		if (isPrimitive() && !(getName().equals("string") || getName().equals("bytes"))) {
			return item.getValue();
		}
		// Not a primitive, read it from the byte stream
		long bucketId = item.getBucketId();
		Long offset = item.getOffset();

		InputStream inputStream = reader.getInputStream(bucketId, offset);
		DataInputStream stream = new DataInputStream(inputStream);
		try {
			if (getName().equals("bytes")) {
				int length = stream.readInt();
				byte[] bytes = new byte[length];
				stream.read(bytes);
				return ByteString.copyFrom(bytes);
			} else if (getName().equals("string")) {
				return readString(stream);
			} else {
				Object value = Class.forName(getName()).newInstance();
				for (org.webseer.model.meta.Field subField : getFields()) {
					// Get the java field
					Field field = value.getClass().getField(subField.getName());

					Object fieldValue = subField.getType().read(stream, field.getType(), subField.isRepeated());

					field.set(value, fieldValue);
				}
				return value;
			}
		} catch (SecurityException e) {
			throw new RuntimeException("Problem reading value", e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Problem reading value", e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Problem reading value", e);
		} catch (IOException e) {
			throw new RuntimeException("Problem reading value", e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Problem reading value", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Problem reading value", e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Problem reading value", e);
		}
	}

	private Object read(DataInputStream input, Class<?> clazz, boolean repeated) throws IOException,
			InstantiationException, IllegalAccessException {
		byte b = input.readByte();
		if (b == 0) {
			return null;
		}
		if (isPrimitive()) {
			if (getName().equals("bytes")) {
				int length = input.readInt();
				byte[] buffer = new byte[length];
				input.read(buffer, 0, length);
				return ByteString.copyFrom(buffer);
			} else if (getName().equals("string")) {
				if (repeated) {
					int length = input.readInt();
					String[] result = new String[length];
					for (int i = 0; i < result.length; i++) {
						result[i] = readString(input);
					}
					return result;
				} else {
					return readString(input);
				}
			} else if (getName().equals("int64")) {
				return input.readLong();
			} else if (getName().equals("int32")) {
				if (repeated) {
					int length = input.readInt();
					int[] result = new int[length];
					for (int i = 0; i < result.length; i++) {
						result[i] = input.readInt();
					}
					return result;
				} else {
					return input.readInt();
				}
			} else if (getName().equals("bool")) {
				return input.readBoolean();
			} else if (getName().equals("double")) {
				return input.readDouble();
			} else {
				throw new RuntimeException("Problem reading value");
			}
		} else {
			Object value = clazz.newInstance();
			for (org.webseer.model.meta.Field subField : getFields()) {
				// Get the java field
				try {
					Field field = clazz.getField(subField.getName());

					Object fieldValue = subField.getType().read(input, field.getType(), subField.isRepeated());

					field.set(value, fieldValue);

				} catch (SecurityException e) {
					throw new RuntimeException("Problem reading value", e);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("Problem reading value", e);
				} catch (NoSuchFieldException e) {
					throw new RuntimeException("Problem reading value", e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Problem reading value", e);
				}
			}
			return value;
		}
	}

	public void setValue(HasValue item, Object value) {
		if (isPrimitive() && !(getName().equals("string") || getName().equals("bytes"))) {
			item.setValue(value);
		} else {
			// Not a primitive, write it to the byte stream
			long bucketId = item.getBucketId();
			Pair<Long, OutputStream> output = reader.getOutputStream(bucketId, item.getOffset());
			item.setOffset(output.getFirst());

			try {
				DataOutputStream outputStream = new DataOutputStream(output.getSecond());
				if (getName().equals("bytes")) {
					byte[] bytes = ((ByteString) value).toByteArray();
					outputStream.writeInt(bytes.length);
					outputStream.write(bytes);
				} else if (getName().equals("string")) {
					item.setValue("string");
					writeString(outputStream, (String) value);
				} else {

					for (org.webseer.model.meta.Field subField : getFields()) {
						// Get the java field
						Field field = value.getClass().getField(subField.getName());

						subField.getType().write(outputStream, field.get(value), subField.isRepeated());

					}
				}
			} catch (SecurityException e) {
				throw new RuntimeException("Problem writing value", e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Problem writing value", e);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException("Problem writing value", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Problem writing value", e);
			} catch (IOException e) {
				throw new RuntimeException("Problem writing value", e);
			}
		}
	}

	private void write(DataOutputStream output, Object value, boolean repeated) throws IOException {
		if (value == null) {
			output.writeByte(0);
			return;
		} else {
			output.writeByte(1);
		}
		if (isPrimitive()) {
			if (getName().equals("bytes")) {
				byte[] bytes = ((ByteString) value).toByteArray();
				output.writeInt(bytes.length);
				output.write(bytes);
			} else if (getName().equals("string")) {
				if (repeated) {
					String[] values = (String[]) value;
					output.writeInt(values.length);
					for (String element : values) {
						writeString(output, element);
					}
				} else {
					writeString(output, ((String) value));
				}
			} else if (getName().equals("int64")) {
				output.writeLong((Long) value);
			} else if (getName().equals("int32")) {
				if (repeated) {
					int[] values = (int[]) value;
					output.writeInt(values.length);
					for (int i = 0; i < values.length; i++) {
						output.writeInt(values[i]);
					}
				} else {
					output.writeInt((Integer) value);
				}
			} else if (getName().equals("bool")) {
				output.writeBoolean((Boolean) value);
			} else if (getName().equals("double")) {
				output.writeDouble((Double) value);
			} else {
				throw new RuntimeException("Not implemented yet");
			}
		} else {
			for (org.webseer.model.meta.Field subField : getFields()) {
				// Get the java field
				try {
					Field field = value.getClass().getField(subField.getName());

					subField.getType().write(output, field.get(value), subField.isRepeated());
				} catch (SecurityException e) {
					throw new RuntimeException("Not implemented yet");
				} catch (IllegalArgumentException e) {
					throw new RuntimeException("Not implemented yet");
				} catch (NoSuchFieldException e) {
					throw new RuntimeException("Not implemented yet");
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Not implemented yet");
				}
			}
		}
	}

	@Override
	public String toString() {
		return "Type [" + getName() + "] (" + underlyingNode.getId() + ")";
	}

	@Override
	public int hashCode() {
		return underlyingNode.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Type other = (Type) obj;
		return underlyingNode.equals(other.underlyingNode);
	}

	public Object getValue(HasValue item, String fieldName) {
		// FIXME: Naive implementation, don't need to create the whole object structure
		Object current = getValue(item);
		if (!StringUtils.isEmpty(fieldName)) {
			String[] path = fieldName.split("\\.");
			Field f = null;
			Class<?> valueClass = current.getClass();
			try {
				for (int i = 0; i < path.length; i++) {
					f = valueClass.getField(path[i]);
					if (i + 1 < path.length) {
						// Have more, need to possibly create
						Object parent = current;
						current = f.get(current);
						if (current == null) {
							current = f.getType().newInstance();
							f.set(parent, current);
						}
						valueClass = current.getClass();
					} else {
						current = f.get(current);
					}
				}
			} catch (SecurityException e) {
				throw new RuntimeException("Problem reading object", e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("Problem reading object", e);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException("Problem reading object", e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Problem reading object", e);
			} catch (InstantiationException e) {
				throw new RuntimeException("Problem reading object", e);
			}
		}
		return current;
	}

	public Type getFieldType(String field) {
		String[] path = field.split("\\.");
		Type current = this;
		for (int i = 0; i < path.length; i++) {
			Type subType = null;
			for (org.webseer.model.meta.Field subfield : current.getFields()) {
				if (subfield.getName().equals(path[i])) {
					subType = subfield.getType();
					break;
				}
			}
			if (subType == null) {
				// Failed to find it
				return null;
			} else {
				current = subType;
			}
		}
		return current;
	}

	private void writeString(DataOutputStream stream, String value) throws IOException {
		byte[] bytes = value.getBytes("UTF-8");
		stream.writeInt(bytes.length);
		stream.write(bytes);
	}

	private String readString(DataInputStream stream) throws IOException {
		int byteLength = stream.readInt();
		byte[] bytes = new byte[byteLength];
		stream.readFully(bytes);
		return new String(bytes, "UTF-8");
	}

	public void deleteBucket(Bucket bucket) {
		if (!isPrimitive() || getName().equals("string") || getName().equals("bytes")) {
			reader.deleteBucket(bucket.getBucketId());
		}
	}

	public void deleteBucket(HasValue bucket) {
		if (!isPrimitive() || getName().equals("string") || getName().equals("bytes")) {
			reader.deleteBucket(bucket.getBucketId());
		}
	}
}
