package org.webseer.model;


public enum NeoRelationshipType implements org.neo4j.graphdb.RelationshipType {
	BUCKET_RUNTIME, BUCKET_NEXT, BUCKET_NODEOUTPUT, BUCKET_FIRST, BUCKET_LAST,

	OUTPUTGROUP_BUCKET, OUTPUTGROUP_TRANSFORMATIONGROUP, OUTPUTGROUP_LAST_ITEM,

	ITEM_OUTPUTGROUP, ITEM_REFERENCE,

	INPUTGROUP_SCOPE, INPUTGROUP_INPUTQUEUE, INPUTGROUP_ITEM, INPUTGROUP_FIRST_ITEM, INPUTGROUP_LAST_ITEM,

	NODEINPUT_INPUTPOINT,

	NODE_NODEINPUT, NODE_EDGE, NODE_TRANSFORMATION, NODE_NODEOUTPUT, NODE_RUNTIME,

	NODEOUTPUT_OUTPUTPOINT,

	USER_WORKSPACE,

	WORKSPACEBUCKET_WORKSPACE, WORKSPACEBUCKET_FIRST_SCOPE, WORKSPACEBUCKET_GRAPH, WORKSPACEBUCKET_LAST_SCOPE, WORKSPACEBUCKET_NEXT, WORKSPACEBUCKET_LAST, WORKSPACEBUCKET_FIRST,

	TRANSFORMATION_INPUTPOINT, TRANSFORMATION_OUTPUTPOINT,

	WORKSPACE_PROGRAM,

	GRAPH_NODE,

	OUTPUTPOINT_MODEL,

	REFERENCE_BUCKETFACTORY, REFERENCE_CONFIGURATION, REFERENCE_USERFACTORY, REFERENCE_WORKSPACEFACTORY,

	BUCKETFACTORY_BUCKET,

	PROGRAM_GRAPH,

	USERFACTORY_USER,

	WORKSPACEFACTORY_WORKSPACE,

	INPUTSCOPE_RUNTIME, INPUTSCOPE_NODEINPUT,

	QUEUE_TARGET, QUEUE_SOURCE, QUEUE_BUCKET, QUEUE_POINTER, QUEUE_RUNTIME,

	SCOPE_INPUTSCOPE, SCOPE_GROUP, SCOPE_NEXT,

	GROUP_NODE,

	REFERENCE_SCOPE, REFERENCE_ITEM, OUTPUTPOINT_TYPE, INPUTPOINT_TYPE, REFERENCE_TYPE_FACTORY, TYPE_FACTORY_TYPE, REFERENCE_TRANSFORMATION_FACTORY, TRANSFORMATION_FACTORY_TRANSFORMATION, WORKSPACE_BUCKET_TYPE, WORKSPACEBUCKETNODE_WORKSPACEBUCKET, WORKSPACEBUCKET_ITEM, TYPE_FIELD, LIST_NEXT, LIST_ITEM, TYPE_FIRST_FIELD, TYPE_LAST_FIELD, FIELD_TYPE, NODEINPUT_FIELD, NODEOUTPUT_FIELD, TRANSFORMATIONFIELD_FIELD, TRANSFORMATIONFIELD_PARENT, NODEINPUT_EDGE, NODEOUTPUT_EDGE, EDGE_FIELD, EDGE_SOURCE, REFERENCE_PREVIEW, QUEUE_WEIR_BUCKET, VIEW_SCOPE, VIEW_DATA, QUEUE_FIRST, QUEUE_LAST, QUEUE_NEXT, VIEW_GROUP, WORKSPACE_BUCKET_GROUP, TRANSFORMATION_FACTORY_BUCKET,
}
