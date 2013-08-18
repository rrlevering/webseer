package org.webseer.bucket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Bucket;
import org.webseer.model.meta.Neo4JMetaUtils;

public class BucketFactory {

	private static Map<GraphDatabaseService, BucketFactory> SINGLETON = new HashMap<GraphDatabaseService, BucketFactory>();

	private final Node underlyingNode;

	public BucketFactory(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public void addBucket(Bucket bucket) {
		addBucket(null, bucket);
	}

	public void addBucket(String user, Bucket bucket) {
		DynamicRelationshipType edge = DynamicRelationshipType.withName(bucket.getName());
		Bucket currentBucket = Neo4JUtils.getLinked(underlyingNode, edge, Bucket.class);
		if (currentBucket != null) {
			throw new RuntimeException("Can only have one bucket per name");
		}
		bucket.setOwner(user);
		registerBucket(bucket);
		this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(bucket), edge);
	}

	void registerBucket(Bucket bucket) {
		GraphDatabaseService service = underlyingNode.getGraphDatabase();
		IndexManager indexManager = service.index();
		Index<Node> index = indexManager.forNodes("buckets");
		index.add(Neo4JMetaUtils.getNode(bucket), "name", bucket.getName());
	}

	public Bucket getBucket(String name) {
		List<Bucket> buckets = searchTransformations(name);
		if (buckets.isEmpty()) {
			return null;
		}
		return buckets.get(0);
	}

	public List<Bucket> searchTransformations(String name) {
		GraphDatabaseService service = underlyingNode.getGraphDatabase();
		IndexManager indexManager = service.index();
		Index<Node> index = indexManager.forNodes("buckets");
		IndexHits<Node> hits = index.query("name", name);
		List<Bucket> results = new ArrayList<>();
		for (Node hit : hits) {
			results.add(Neo4JUtils.getInstance(hit, Bucket.class));
		}
		return results;
	}

	public Iterable<Bucket> getAllBuckets() {
		return Neo4JUtils.getIterable(underlyingNode, Bucket.class);
	}

	public void removeBucket(Bucket bucket) {
		Neo4JMetaUtils.getNode(bucket)
				.getSingleRelationship(NeoRelationshipType.TRANSFORMATION_FACTORY_TRANSFORMATION, Direction.INCOMING)
				.delete();
	}

	public static BucketFactory getBucketFactory(GraphDatabaseService service) {
		if (!SINGLETON.containsKey(service)) {
			BucketFactory factory = Neo4JUtils.getSingleton(service, BucketFactory.class);
			SINGLETON.put(service, factory);
			return factory;
		}
		return SINGLETON.get(service);
	}

}
