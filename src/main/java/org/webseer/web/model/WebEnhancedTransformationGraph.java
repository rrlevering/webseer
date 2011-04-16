package org.webseer.web.model;

import org.neo4j.api.core.Direction;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.program.TransformationGraph;

public class WebEnhancedTransformationGraph extends TransformationGraph {

	private WebEnhancedTransformationGraph(Node underlyingNode) {
		super(underlyingNode);
	}

	protected Node getUnderlyingNode() {
		return super.getUnderlyingNode();
	}

	public static WebEnhancedTransformationGraph get(NeoService service, long id) {
		Node node = service.getNodeById(id);
		if (!node.hasRelationship(WebRelationshipType.GRAPH_UI)) {
			Node uiNode = Neo4JUtils.createNode(service);
			node.createRelationshipTo(uiNode, WebRelationshipType.GRAPH_UI);
		}
		return new WebEnhancedTransformationGraph(node);
	}

	public GraphUI getUI() {
		return GraphUI.wrap(getUnderlyingNode().getSingleRelationship(WebRelationshipType.GRAPH_UI, Direction.OUTGOING)
				.getEndNode());
	}

}
