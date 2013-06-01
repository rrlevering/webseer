package org.webseer.web.ajax;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.sonatype.aether.RepositoryException;
import org.webseer.model.NeoRelationshipType;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Neo4JMetaUtils;
import org.webseer.model.meta.Transformation;
import org.webseer.repository.Repository;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.EditTransformation.DependencyBean;
import org.webseer.web.beans.UserBean;
import org.webseer.web.SeerServlet;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class UploadDependency extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	@Override
	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {

		UserBean currentUser = (UserBean) req.getSession().getAttribute("user");
		
		// Create a factory for disk-based file items
		DiskFileItemFactory fileFactory = new DiskFileItemFactory();

		// Set factory constraints
		fileFactory.setSizeThreshold(25000000);

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(fileFactory);

		// Parse the request
		List<FileItem> items;
		try {
			items = upload.parseRequest(req);
		} catch (FileUploadException e) {
			throw new ServletException(e);
		}

		// Process the uploaded items
		InputStream fileStream = null;
		String group = currentUser.getLogin().replaceAll("[\\.\\@]", "_"), name = null, version = null;
		Iterator<FileItem> iter = items.iterator();
		while (iter.hasNext()) {
			FileItem item = iter.next();

			if (item.isFormField()) {
				if (item.getFieldName().equals("artifactId")) {
					name = item.getString();
				} else if (item.getFieldName().equals("version")) {
					version = item.getString();
				}
			} else {
				if (!item.getContentType().equals("application/java-archive")) {
					throw new ServletException("Can only upload jar files");
				}
				fileStream = item.getInputStream(); // The stream of the uploaded jar
			}
		}

		if (group == null || name == null || version == null || fileStream == null) {
			throw new ServletException("Can't proceed");
		}

		try {
			Repository.getDefaultInstance().uploadArtifact(group, name, version, fileStream);
		} catch (RepositoryException e) {
			throw new ServletException("Unable to upload archive", e);
		}
		
		String transformationPath = (String) getRequiredAttribute(req, "transformationPath");
		String transformName = transformationPath.replace('/', '.');

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		Transformation transformation = factory.getLatestTransformationByName(transformName);

		Library library = new Library(getNeoService(), group, name, version);
		Neo4JMetaUtils.getNode(transformation).createRelationshipTo(Neo4JMetaUtils.getNode(library),
				NeoRelationshipType.TRANSFORMATION_LIBRARY);

		DependencyBean dependency = new DependencyBean(group, name, version);
		
		JsonObject responseObject = new JsonObject();
		responseObject.add("id", new JsonPrimitive(dependency.getId()));
		responseObject.add("safeId", new JsonPrimitive(dependency.getSafeId()));
		
		Writer writer = resp.getWriter();
		writer.write(responseObject.toString());
		writer.close();

	}
}
