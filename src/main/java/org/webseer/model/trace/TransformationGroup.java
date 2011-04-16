package org.webseer.model.trace;

import name.levering.ryan.util.IterableUtils;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.program.TransformationNodeInput;
import org.webseer.model.runtime.Neo4JRuntimeUtils;
import org.webseer.model.runtime.RuntimeTransformationNode;

public class TransformationGroup {

	public static final String START_TIME = "START_TIME";

	public static final String END_TIME = "END_TIME";

	private final Node underlyingNode;

	public TransformationGroup(NeoService service, RuntimeTransformationNode node) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		this.underlyingNode.createRelationshipTo(Neo4JRuntimeUtils.getNode(node), NeoRelationshipType.GROUP_NODE);
	}

	public TransformationGroup(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public long getTransformationGroupId() {
		return underlyingNode.getId();
	}

	public RuntimeTransformationNode getRuntimeNode() {
		return Neo4JUtils.getLinked(this.underlyingNode, NeoRelationshipType.GROUP_NODE,
				RuntimeTransformationNode.class);
	}

	public Iterable<InputGroup> getInputGroups() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.SCOPE_GROUP, InputGroup.class);
	}

	public Iterable<OutputGroup> getOutputGroups() {
		return Neo4JUtils.getIterable(underlyingNode, NeoRelationshipType.OUTPUTGROUP_TRANSFORMATIONGROUP,
				OutputGroup.class);
	}

	Node getUnderlyingNode() {
		return this.underlyingNode;
	}

	public void delete() {
		// Remove this group from the runtime node
		RuntimeTransformationNode runtime = getRuntimeNode();
		underlyingNode.getSingleRelationship(NeoRelationshipType.GROUP_NODE, Direction.OUTGOING).delete();
		System.out.println("Deleting tgroup");

		// Check whether we can clean up the bucket object
		runtime.checkForDelete();

		// We can automatically clean up the input groups
		for (InputGroup iGroup : getInputGroups()) {
			iGroup.delete();
		}
		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}
		underlyingNode.delete();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof TransformationGroup)) {
			return false;
		}
		return ((TransformationGroup) o).getUnderlyingNode().equals(underlyingNode);
	}

	public String toString() {
		return "TransformationGroup[" + getRuntimeNode().getTransformationNode().getTransformation().getName() + "]";
	}

	public long getNodeId() {
		return underlyingNode.getId();
	}

	public static TransformationGroup get(NeoService service, long transformationNodeId) {
		return new TransformationGroup(service.getNodeById(transformationNodeId));
	}

	public InputGroup getInputGroup(NeoService service, TransformationNodeInput input) {
		for (InputGroup iGroup : getInputGroups()) {
			if (iGroup.getInputQueue().getInput().equals(input)) {
				return iGroup;
			}
		}
		return new InputGroup(service, getRuntimeNode().getQueue(input.getInputField().getName()), this);
	}

	public OutputGroup getOutputGroup(NeoService service, Bucket bucket) {
		for (OutputGroup oGroup : getOutputGroups()) {
			if (oGroup.getBucket().equals(bucket)) {
				return oGroup;
			}
		}
		return new OutputGroup(service, bucket, this);
	}

	public void setStartTime(long time) {
		this.underlyingNode.setProperty(START_TIME, time);
	}

	public Long getStartTime() {
		return Neo4JUtils.getLong(underlyingNode, START_TIME);
	}

	public void setEndTime(long time) {
		this.underlyingNode.setProperty(END_TIME, time);
	}

	public Long getEndTime() {
		return Neo4JUtils.getLong(underlyingNode, END_TIME);
	}

	public void checkForDelete() {
		if (IterableUtils.size(getOutputGroups()) == 0) {
			delete();
		}
	}

}
