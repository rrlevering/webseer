package org.webseer.model.runtime;

import name.levering.ryan.util.IterableUtils;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.webseer.logging.LocalLog;
import org.webseer.logging.Log;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.program.DisconnectedWorkspaceBucketNode;
import org.webseer.model.program.Neo4JProgramUtils;
import org.webseer.model.program.TransformationNode;
import org.webseer.model.program.TransformationNodeOutput;
import org.webseer.model.trace.Bucket;
import org.webseer.model.trace.TransformationGroup;
import org.webseer.transformation.LanguageFactory;
import org.webseer.transformation.PullRuntimeTransformation;

/**
 * Runtime transformation node is used for the actual runtime structure of a transformation graph. Transformation graphs
 * can have cycles, whereas runtime transformation graphs are more like an execution stack and are DAGs. Runtime
 * transformations have buckets for outputs and input queues for inputs. The bucket is an ordered set of items and the
 * input queue is a pointer to a position in a previous bucket. On a lower level, these items have back edges to the
 * inputs that they were generated from.
 * <p>
 * The tricky part is the jump from bucket to the push to the input queues. When buckets have data to push, they need to
 * figure out the correct input queue to notify. This requires aligning the graph to connect to the correct runtime
 * transformation node. Let's say we have a cycle: S -> A -> B -> C -> A or T. The good thing is that we can just keep
 * track of the "current" runtime node and align on that because we never create the graph until the past ancestors are
 * already running. So we keep track of the current runtime node for a particular transformation node and then when a
 * runtime node starts running, we clear the edge on the transformation node.
 * 
 */
public class RuntimeTransformationNode {

	public static final LocalLog LOG = Log.getInstance("RUNTIME");

	public static final String TRANSFORMATION_NODE = "TRANSFORMATION_NODE";

	protected final Node underlyingNode;

	public RuntimeTransformationNode(NeoService service, TransformationNode node) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(Neo4JProgramUtils.getNode(node), NeoRelationshipType.NODE_RUNTIME);

		for (TransformationNodeOutput output : node.getOutputs()) {
			if (IterableUtils.size(output.getOutgoingEdges()) > 0) {
				new Bucket(service, this, output);
			}
		}
	}

	public RuntimeTransformationNode(NeoService service, DisconnectedWorkspaceBucketNode node) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(Neo4JProgramUtils.getNode(node), NeoRelationshipType.NODE_RUNTIME);
	}

	public RuntimeTransformationNode(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public PullRuntimeTransformation getPullTransformation(RuntimeConfiguration config) throws TransformationException {
		LanguageFactory factory = LanguageFactory.getInstance();
		return factory.generatePullTransformation(config, this);
	}

	/**
	 * The buckets are the storage places for output points of this transformation.
	 * 
	 * @return an iterable over the buckets
	 */
	public Iterable<Bucket> getBuckets() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.BUCKET_RUNTIME, Bucket.class);
	}

	/**
	 * The input queues are pointers at previous buckets that we're pulling data from.
	 */
	public Iterable<InputQueue> getQueues() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.QUEUE_RUNTIME, InputQueue.class);
	}

	public TransformationNode getTransformationNode() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.NODE_RUNTIME, TransformationNode.class);
	}

	public Iterable<TransformationGroup> getGroups() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.GROUP_NODE, TransformationGroup.class);
	}

	Node getUnderlyingNode() {
		return underlyingNode;
	}

	public String toString() {
		return "Runtime-" + getTransformationNode().toString();
	}

	public long getNodeId() {
		return underlyingNode.getId();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof RuntimeTransformationNode)) {
			return false;
		}
		return ((RuntimeTransformationNode) o).getUnderlyingNode().equals(underlyingNode);
	}

	/**
	 * Only returns created and already attached input queues.
	 */
	public InputQueue getQueue(String input) {
		for (InputQueue queue : getQueues()) {
			if (queue.getInput().getInputField().getName().equals(input)) {
				return queue;
			}
		}
		return null;
	}

	public Bucket getBucket(String sourceOutput) {
		for (Bucket bucket : getBuckets()) {
			if (bucket.getTransformationNodeOutput().getOutputField().getName().equals(sourceOutput)) {
				return bucket;
			}
		}
		return null;
	}

	public void checkForDelete() {
		if (IterableUtils.size(getGroups()) == 0) {
			delete();
		}
	}

	public void delete() {
		System.out.println("Deleting runtime node");

		// Check the transformation level for deletes
		TransformationNode node = getTransformationNode();
		underlyingNode.getSingleRelationship(NeoRelationshipType.NODE_RUNTIME, Direction.OUTGOING).delete();
		node.delete();

		// Delete all input queue relationships
		for (Relationship rel : underlyingNode.getRelationships(NeoRelationshipType.QUEUE_RUNTIME)) {
			rel.delete();
		}

		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}
		underlyingNode.delete();
	}

}
