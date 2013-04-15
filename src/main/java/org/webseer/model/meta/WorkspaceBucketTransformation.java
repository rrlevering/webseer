package org.webseer.model.meta;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.transformation.PullRuntimeTransformation;

public class WorkspaceBucketTransformation extends Transformation {
	
	public WorkspaceBucketTransformation(GraphDatabaseService service) {
		super(Neo4JUtils.createNode(service, WorkspaceBucketTransformation.class), "Workspace Bucket");
		new InputPoint(service, this, new Field(service, new Type(service, "any"), "itemToAdd", true));
		new OutputPoint(service, this, new Field(service, new Type(service, "any"), "itemToOutput", true));
	}
	
	public WorkspaceBucketTransformation(Node node) {
		super(node);
	}

	@Override
	public PullRuntimeTransformation getPullRuntimeTransformation() {
		return null;
	}

	@Override
	public String getType() {
		return "Workspace Bucket";
	}

}
