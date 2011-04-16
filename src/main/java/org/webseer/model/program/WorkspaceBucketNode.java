package org.webseer.model.program;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.WorkspaceBucket;
import org.webseer.transformation.TransformationFactory;

/**
 * Workspace buckets are special cases because they have direct access to workspace buckets and in scope act like a
 * single transformation. Otherwise, they write out items that pass on their scope chains from when they were written.
 * 
 */
public class WorkspaceBucketNode extends TransformationNode {

	public WorkspaceBucketNode(NeoService service, TransformationGraph graph, WorkspaceBucket bucket) {
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
