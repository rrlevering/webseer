package org.webseer.type;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.meta.TransformationException;
import org.webseer.model.meta.Type;

public interface LanguageTypeFactory {

	/**
	 * Generates a meta-class, representing a particular data structure in the underlying language.
	 * @throws TransformationException 
	 */
	public Collection<Type> generateTypes(GraphDatabaseService service, String typeName, InputStream input, long version)
			throws IOException, TransformationException;
	
}
