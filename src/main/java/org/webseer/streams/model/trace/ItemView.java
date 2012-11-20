package org.webseer.streams.model.trace;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.UserType;

/**
 * The result of an input filter on an item. You can cast any item back into its context to pull things from earlier in
 * the stream. Inputs to functions have a scope that is different than the actual data that is passed in.
 */
public class ItemView {

	private final Node underlyingNode;

	public ItemView(GraphDatabaseService service, Item scope, Item data, String field, InputGroup group) {
		this.underlyingNode = Neo4JUtils.createNode(service);

		if (scope != null) { // scope can be null for config values
			this.underlyingNode.createRelationshipTo(scope.getUnderlyingNode(), NeoRelationshipType.VIEW_SCOPE);
		}
		this.underlyingNode.createRelationshipTo(data.getUnderlyingNode(), NeoRelationshipType.VIEW_DATA);
		if (field != null) {
			this.underlyingNode.setProperty("DATA_FIELD", field);
		}
		this.underlyingNode.createRelationshipTo(group.getUnderlyingNode(), NeoRelationshipType.VIEW_GROUP);
	}

	public ItemView(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public InputGroup getInputGroup() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.VIEW_GROUP, InputGroup.class);
	}

	public Item getViewScope() {
		if (!underlyingNode.hasRelationship(NeoRelationshipType.VIEW_SCOPE, Direction.OUTGOING)) {
			return null;
		}
		return Neo4JUtils.wrapItem(this.underlyingNode.getSingleRelationship(NeoRelationshipType.VIEW_SCOPE,
				Direction.OUTGOING).getEndNode());
	}

	public Item getViewData() {
		if (!underlyingNode.hasRelationship(NeoRelationshipType.VIEW_DATA, Direction.OUTGOING)) {
			return null;
		}
		return Neo4JUtils.wrapItem(this.underlyingNode.getSingleRelationship(NeoRelationshipType.VIEW_DATA,
				Direction.OUTGOING).getEndNode());
	}

	public Object get() {
		if (getDataField() != null) {
			return getViewData().get(getDataField());
		}
		return getViewData().get();
	}

	public String getDataField() {
		return Neo4JUtils.getString(underlyingNode, "DATA_FIELD");
	}

	public UserType getType() {
		if (getDataField() != null) {
			return getViewData().getType(getDataField());
		}
		return getViewData().getType();
	}

	public Node getUnderlyingNode() {
		return underlyingNode;
	}

	public long getId() {
		return underlyingNode.getId();
	}

	@Override
	public int hashCode() {
		return underlyingNode.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ItemView)) {
			return false;
		}
		return ((ItemView) o).getUnderlyingNode().equals(underlyingNode);
	}

	public void delete() {
		System.out.println("Deleting item view");

		Item scope = getViewScope();
		if (scope != null) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.VIEW_SCOPE, Direction.OUTGOING).delete();
			scope.getOutputGroup().checkForDelete();
		}

		Item data = getViewData();
		if (data != null) {
			underlyingNode.getSingleRelationship(NeoRelationshipType.VIEW_DATA, Direction.OUTGOING).delete();
			if (data.getOutputGroup() != null) {
				data.getOutputGroup().checkForDelete();
			} // TODO: Handle TNI case differently?
		}

		underlyingNode.getSingleRelationship(NeoRelationshipType.VIEW_GROUP, Direction.OUTGOING).delete();

		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}
		underlyingNode.delete();
	}
}
