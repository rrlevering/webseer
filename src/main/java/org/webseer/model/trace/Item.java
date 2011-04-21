package org.webseer.model.trace;

import org.neo4j.graphdb.Node;

public interface Item extends Data {

	Node getUnderlyingNode();

	long getItemId();

	OutputGroup getOutputGroup();

	void delete();

}
