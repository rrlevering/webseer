package org.webseer.type;

import java.io.IOException;
import java.io.Reader;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.meta.Type;

public interface LanguageTypeFactory {

	/**
	 * Generates a meta-class, representing a particular data structure in the underlying language.
	 */
	public Type generateType(GraphDatabaseService service, String typeName, Reader reader, long version)
			throws IOException;
	
}
