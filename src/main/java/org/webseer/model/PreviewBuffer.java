package org.webseer.model;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.DynamicRelationshipType;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;

/**
 * This is used to keep track of all the preview buckets so they can be cleaned up. We hold each transformation edge
 * preview that's clicked in memory for some time before releasing the on-disk memory.
 */
public class PreviewBuffer {

	private final Node underlyingNode;

	public PreviewBuffer(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public static PreviewBuffer getPreviewBuffer(NeoService service) {
		return Neo4JUtils.getSingleton(service, NeoRelationshipType.REFERENCE_PREVIEW, PreviewBuffer.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public WorkspaceBucket getPreviewBucket(NeoService service, String sessionId) {
		if (!underlyingNode.hasRelationship(DynamicRelationshipType.withName(sessionId), Direction.OUTGOING)) {
			WorkspaceBucket bucket = new WorkspaceBucket(service, this);
			underlyingNode
					.createRelationshipTo(bucket.getUnderlyingNode(), DynamicRelationshipType.withName(sessionId));
		}
		return new WorkspaceBucket(underlyingNode.getSingleRelationship(DynamicRelationshipType.withName(sessionId),
				Direction.OUTGOING).getEndNode());
	}

}
