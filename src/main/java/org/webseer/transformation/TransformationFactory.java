package org.webseer.transformation;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.InputType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.type.TypeFactory;

public class TransformationFactory {

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
				Bootstrapper.bootstrapBuiltins(service);
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


}
