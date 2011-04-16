package org.webseer.web.model;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.webseer.model.Neo4JUtils;

public class GraphUI {

	private final static String SNAPSHOT = "snapshot";

	private final static String TRANSACTIONS = "transactions";

	private final Node underlyingNode;

	public GraphUI(NeoService service, WebEnhancedTransformationGraph graph) {
		this.underlyingNode = Neo4JUtils.createNode(service);
		graph.getUnderlyingNode().createRelationshipTo(this.underlyingNode, WebRelationshipType.GRAPH_UI);
	}

	private GraphUI(Node underlyingNode) {
		this.underlyingNode = underlyingNode;
	}

	public String getGraphSnapshot() {
		if (!underlyingNode.hasProperty(SNAPSHOT)) {
			return null;
		}
		return (String) underlyingNode.getProperty(SNAPSHOT);
	}

	public void setGraphSnapshot(String snapshot) {
		underlyingNode.setProperty(SNAPSHOT, snapshot);
	}

	public String getGraphDeltas() {
		if (!underlyingNode.hasProperty(TRANSACTIONS)) {
			return null;
		}
		return (String) underlyingNode.getProperty(TRANSACTIONS);
	}

	public void setGraphDeltas(String deltas) {
		underlyingNode.setProperty(TRANSACTIONS, deltas);
	}

	public static GraphUI wrap(Node node) {
		return new GraphUI(node);
	}

}
