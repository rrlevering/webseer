package org.webseer.web.ajax;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.Relationship;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Transformation;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.SeerServlet;

import com.google.gson.JsonObject;

public class RemoveDependency extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		String dependencyIdToRemove = getRequiredParameter(req, "dependency");

		String transformationPath = (String) getRequiredAttribute(req, "transformationPath");
		String name = transformationPath.replace('/', '.');

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		Transformation transformation = factory.getLatestTransformationByName(name);

		if (dependencyIdToRemove != null && !dependencyIdToRemove.isEmpty()) {
			for (Relationship rel : Neo4JMetaUtils.getNode(transformation).getRelationships(
					NeoRelationshipType.TRANSFORMATION_LIBRARY)) {
				Library library = Neo4JUtils.getInstance(rel.getEndNode(), Library.class);
				if (dependencyIdToRemove.equals(library.getGroup() + ":" + library.getName() + ":"
						+ library.getVersion())) {
					rel.delete();
				}
			}
		}

		JsonObject response = new JsonObject();

		Writer writer = resp.getWriter();
		writer.write(response.toString());
		writer.close();

	}

}
