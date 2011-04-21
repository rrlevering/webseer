package org.webseer.model.program;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.WorkspaceBucket;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Type;

/**
 * Workspace buckets are special cases because they have direct access to workspace buckets and in scope act like a
 * single transformation. Otherwise, they write out items that pass on their scope chains from when they were written.
 * 
 */
public class DisconnectedWorkspaceBucketNode extends TransformationNode {

	static final String BUCKET_NAME = "BUCKET_NAME";

	public DisconnectedWorkspaceBucketNode(Node node) {
		super(node);
	}

	public DisconnectedWorkspaceBucketNode(WorkspaceBucketNode asBucket) {
		super(asBucket.underlyingNode);

		WorkspaceBucket bucket = asBucket.getLinkedBucket();
		underlyingNode.setProperty(BUCKET_NAME, bucket.getName());
		// Remove the link to the workspace bucket
		this.underlyingNode.getSingleRelationship(NeoRelationshipType.WORKSPACEBUCKETNODE_WORKSPACEBUCKET,
				Direction.OUTGOING).delete();
		if (bucket.getType() != null) {
			this.underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(bucket.getType()),
					NeoRelationshipType.WORKSPACE_BUCKET_TYPE);
		}

	}

	public String getLinkedBucketName() {
		return (String) underlyingNode.getProperty(BUCKET_NAME);
	}

	public Type getType() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.WORKSPACE_BUCKET_TYPE, Type.class);
	}
}
