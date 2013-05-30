package org.webseer.web.ajax;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.aether.RepositoryException;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Transformation;
import org.webseer.repository.Repository;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.SeerServlet;

import com.google.gson.JsonObject;

public class AddDependency extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		String newDependencyId = getRequiredParameter(req, "dependency");
		
		String transformationPath = (String) getRequiredAttribute(req, "transformationPath");
		String name = transformationPath.replace('/', '.');

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		Transformation transformation = factory.getLatestTransformationByName(name);
		
		if (newDependencyId != null && !newDependencyId.isEmpty()) {
			try {
				File artifact = Repository.getDefaultInstance().getArtifact(newDependencyId);
				if (artifact != null) {
					String[] idSplit = newDependencyId.split(":");
					Library library = new Library(getNeoService(), idSplit[0], idSplit[1], idSplit[2]);
					Neo4JMetaUtils.getNode(transformation).createRelationshipTo(Neo4JMetaUtils.getNode(library),
							NeoRelationshipType.TRANSFORMATION_LIBRARY);
				}
			} catch (RepositoryException e) {
				throw new ServletException("Problem with repository", e);
			}
		}
		
		JsonObject response = new JsonObject();

		Writer writer = resp.getWriter();
		writer.write(response.toString());
		writer.close();

	}

}
