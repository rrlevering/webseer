package org.webseer.transformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.WorkspaceBucketTransformation;

public class TransformationFactory {

	private static Map<GraphDatabaseService, TransformationFactory> SINGLETON = new HashMap<GraphDatabaseService, TransformationFactory>();

	private final Node underlyingNode;

	public TransformationFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public void addTransformation(Transformation transformation) {
		addTransformation(null, transformation);
	}

	public void addTransformation(String user, Transformation transformation) {
		DynamicRelationshipType edge = DynamicRelationshipType.withName(transformation.getName());
		Transformation currentTransformation = Neo4JUtils.getLinked(underlyingNode, edge, Transformation.class);
		if (currentTransformation != null) {
			String currentOwner = currentTransformation.getOwner();
			if (currentOwner != null && !currentOwner.equals(user)) {
				// Disallowed
				return;
			}
			Relationship rel = underlyingNode.getSingleRelationship(edge, Direction.OUTGOING);
			rel.delete();
			Neo4JMetaUtils.getNode(transformation).createRelationshipTo(Neo4JMetaUtils.getNode(currentTransformation),
					NeoRelationshipType.TRANSFORMATION_LAST_VERSION);
		}
		transformation.setOwner(user);
		registerTransformation(transformation);
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(transformation), edge);
	}

	void registerTransformation(Transformation transformation) {
		GraphDatabaseService service = underlyingNode.getGraphDatabase();
		IndexManager indexManager = service.index();
		Index<Node> index = indexManager.forNodes("transformations");
		index.add(Neo4JMetaUtils.getNode(transformation), "name", transformation.getName());
	}
	
	public List<Transformation> searchTransformations(String name) {
		GraphDatabaseService service = underlyingNode.getGraphDatabase();
		IndexManager indexManager = service.index();
		Index<Node> index = indexManager.forNodes("transformations");
		IndexHits<Node> hits = index.query("name", name);
		List<Transformation> results = new ArrayList<>();
		for (Node hit : hits) {
			results.add(Neo4JUtils.getInstance(hit, Transformation.class));
		}
		return results;
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
					TransformationFactory.class);
			SINGLETON.put(service, factory);
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
		DynamicRelationshipType edge = DynamicRelationshipType.withName(name);
		return Neo4JUtils.getLinked(underlyingNode, edge, Transformation.class);
	}

	public Transformation getTransformation(long id) {
		for (Transformation transform : getAllTransformations()) {
			if (Neo4JMetaUtils.getNode(transform).getId() == id) {
				return transform;
			}
		}
		return null;
	}

	public Iterable<Transformation> getAllTransformations() {
		return Neo4JUtils.getIterable(underlyingNode, Transformation.class);
	}

	public String toString() {
		return "TransformationFactory[" + underlyingNode.getId() + "]";
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
			Transformation trans = new WorkspaceBucketTransformation(service);
			underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(trans),
					NeoRelationshipType.TRANSFORMATION_FACTORY_BUCKET);
		}
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATION_FACTORY_BUCKET,
				Transformation.class);
	}

}
