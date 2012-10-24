package org.webseer.streams.model.program;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.streams.model.WorkspaceBucket;
import org.webseer.transformation.TransformationFactory;

/**
 * Workspace bucket nodes are special cases because they have direct access to workspace buckets and in scope act like a
 * single transformation. Otherwise, they write out items that pass on their scope chains from when they were written.
 * 
 */
public class WorkspaceBucketNode extends TransformationNode {

	public WorkspaceBucketNode(GraphDatabaseService service, TransformationGraph graph, WorkspaceBucket bucket) {
		super(service, TransformationFactory.getTransformationFactory(service).getBucketTransformation(service), graph);
		this.underlyingNode.createRelationshipTo(Neo4JUtils.getNode(bucket),
				NeoRelationshipType.WORKSPACEBUCKETNODE_WORKSPACEBUCKET);
	}

	public WorkspaceBucketNode(Node node) {
		super(node);
	}

	public WorkspaceBucket getLinkedBucket() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.WORKSPACEBUCKETNODE_WORKSPACEBUCKET,
				WorkspaceBucket.class);
	}
}
