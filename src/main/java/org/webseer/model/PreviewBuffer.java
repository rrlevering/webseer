package org.webseer.model;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * This is used to keep track of all the preview buckets so they can be cleaned up. We hold each transformation edge
 * preview that's clicked in memory for some time before releasing the on-disk memory.
 */
public class PreviewBuffer {

	private final Node underlyingNode;

	public PreviewBuffer(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public static PreviewBuffer getPreviewBuffer(GraphDatabaseService service) {
		return Neo4JUtils.getSingleton(service, NeoRelationshipType.REFERENCE_PREVIEW, PreviewBuffer.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public WorkspaceBucket getPreviewBucket(GraphDatabaseService service, String sessionId) {
		if (!underlyingNode.hasRelationship(DynamicRelationshipType.withName(sessionId), Direction.OUTGOING)) {
			WorkspaceBucket bucket = new WorkspaceBucket(service, this);
			underlyingNode
					.createRelationshipTo(bucket.getUnderlyingNode(), DynamicRelationshipType.withName(sessionId));
		}
		return new WorkspaceBucket(underlyingNode.getSingleRelationship(DynamicRelationshipType.withName(sessionId),
				Direction.OUTGOING).getEndNode());
	}

}
