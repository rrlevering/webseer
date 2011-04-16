package org.webseer.model.trace;

import org.neo4j.api.core.Node;

public interface Item extends Data {

	Node getUnderlyingNode();

	long getItemId();

	OutputGroup getOutputGroup();

	void delete();

}
