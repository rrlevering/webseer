package org.webseer.model.trace;

import java.io.InputStream;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Type;

/**
 * References allow you to have pointers to a previous place in the scope, which allow you to do things like aggregate
 * filters (where all scopes are consumed and then certain scopes are output...think maximum functions). Normal scope
 * search would mean that any output scopes would have all scopes in their history, thus making the scope unusable.
 */
public class Reference implements Item {

	private final Node underlyingNode;

	public Reference(NeoService service, OutputGroup outputGroup, ItemView item) {
		this.underlyingNode = Neo4JUtils.createNode(service, Reference.class);

		this.underlyingNode.createRelationshipTo(item.getUnderlyingNode(), NeoRelationshipType.REFERENCE_ITEM);
		this.underlyingNode.createRelationshipTo(outputGroup.getUnderlyingNode(), NeoRelationshipType.ITEM_OUTPUTGROUP);
	}

	public Reference(Node nextNode) {
		this.underlyingNode = nextNode;
	}

	public ItemView getReferencedView() {
		return Neo4JUtils.getLinked(this.underlyingNode, NeoRelationshipType.REFERENCE_ITEM, ItemView.class);
	}

	public OutputGroup getOutputGroup() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.ITEM_OUTPUTGROUP, OutputGroup.class);
	}

	public Node getUnderlyingNode() {
		return underlyingNode;
	}

	public String toString() {
		return "->" + getReferencedView();
	}

	public long getItemId() {
		return underlyingNode.getId();
	}

	public Object get() {
		return getReferencedView().get();
	}

	@Override
	public Type getType() {
		return getReferencedView().getType();
	}

	@Override
	public Object get(String field) {
		return getReferencedView().getViewData().get(field);
	}

	@Override
	public Type getType(String field) {
		return getReferencedView().getViewData().getType(field);
	}

	@Override
	public InputStream getInputStream() {
		return getReferencedView().getViewData().getInputStream();
	}

	public int hashCode() {
		return this.underlyingNode.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Reference)) {
			return false;
		}
		return ((Reference) o).getUnderlyingNode().equals(underlyingNode);
	}

	public void delete() {
		System.out.println("Deleting " + this);

		// Remove it from the bucket
		Neo4JUtils.removeFromInlineList(underlyingNode, NeoRelationshipType.BUCKET_FIRST,
				NeoRelationshipType.BUCKET_LAST, NeoRelationshipType.BUCKET_NEXT);

		for (Relationship rel : underlyingNode.getRelationships()) {
			System.out.println(rel.getType());
			rel.delete();
		}
		underlyingNode.delete();
	}

}
