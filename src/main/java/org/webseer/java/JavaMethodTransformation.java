package org.webseer.java;

import java.lang.reflect.Method;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.PullRuntimeTransformation;

public class JavaMethodTransformation extends Transformation {

	public JavaMethodTransformation(GraphDatabaseService db, String name, Library library, Method m) {
		super(Neo4JUtils.createNode(db, JavaMethodTransformation.class), name);
		underlyingNode.createRelationshipTo(Neo4JMetaUtils.getNode(library), NeoRelationshipType.TRANSFORMATION_LIBRARY);
		
		String identifier = JavaRuntimeFactory.serializeMethod(m);
		underlyingNode.setProperty("IDENTIFIER", identifier);
	}
	
	public Library getLibrary() {
		return Neo4JUtils.getLinked(underlyingNode, NeoRelationshipType.TRANSFORMATION_LIBRARY, Library.class);
	}
	
	@Override
	public PullRuntimeTransformation getPullRuntimeTransformation() throws TransformationException {
		String identifier = Neo4JUtils.getString(underlyingNode, "IDENTIFIER");
		try {
			Method method = JavaRuntimeFactory.getDefaultInstance().getMethod(identifier, getLibrary());
			
			// Load and wrap the object with a java transformation wrapper
			return new MethodTransformation(this, method, null);
		} catch (Exception e) {
			throw new TransformationException("Unable to instantiate user code", e);
		}
	}

	@Override
	public String getType() {
		return "Java Library Function";
	}

}
