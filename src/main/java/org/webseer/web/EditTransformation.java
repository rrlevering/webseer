package org.webseer.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.sonatype.aether.RepositoryException;
import org.webseer.java.JavaRuntimeFactory;
import org.webseer.java.JavaRuntimeFactory.CompilationFailedException;
import org.webseer.java.JavaTransformation;
import org.webseer.model.meta.FileVersion;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.repository.Repository;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.beans.UserBean;

public class EditTransformation extends SeerServlet {

	private static final long serialVersionUID = -7750239856212311310L;

	public static class DependencyBean {

		private String version;
		private String group;
		private String name;

		public DependencyBean(String group, String name, String version) {
			this.group = group;
			this.name = name;
			this.version = version;
		}

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}

	@Override
	protected void transactionalizedGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String transformationPath = (String) getRequiredAttribute(request, "transformationPath");
		String transformationName = transformationPath.replace('/', '.');

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);

		JavaTransformation transformation = (JavaTransformation) factory
				.getLatestTransformationByName(transformationName);

		System.out.println("Got transformation " + transformation + " from " + factory);
		request.setAttribute("name", transformationName);

		if (transformation != null) {
			// Populate information from the transformation
			request.setAttribute("currentSource", transformation.getSource().getCode());
			request.setAttribute("version", transformation.getVersion());

			List<DependencyBean> dependencies = new ArrayList<DependencyBean>();
			for (Library library : transformation.getLibraries()) {
				dependencies.add(new DependencyBean(library.getGroup(), library.getName(), library.getVersion()));
			}

			request.setAttribute("dependencies", dependencies);
		} else {
			request.setAttribute("version", "0");
		}

		RequestDispatcher rd = request.getRequestDispatcher("edit-transformation.jsp");
		rd.forward(request, response);
	}

	@Override
	protected void transactionalizedPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String transformationPath = (String) getRequiredAttribute(request, "transformationPath");
		String transformationName = transformationPath.replace('/', '.');

		String action = (String) getRequiredParameter(request, "action");
		if (action.equals("Cancel")) {
			response.sendRedirect(request.getContextPath() + "/transformation/" + transformationPath);
			return;
		}
		
		String source = request.getParameter("source");

		request.setAttribute("name", transformationName);
		request.setAttribute("currentSource", source);

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		Transformation transformation = factory.getLatestTransformationByName(transformationName);

		System.out.println("Got transformation " + transformation + " from " + factory);
		request.setAttribute("version", 0);

		boolean changed = false;
		List<DependencyBean> dependencies = new ArrayList<DependencyBean>();

		List<Library> libraries = new ArrayList<Library>();
		if (transformation instanceof JavaTransformation) {
			for (Library library : ((JavaTransformation) transformation).getLibraries()) {
				libraries.add(library);
				dependencies.add(new DependencyBean(library.getGroup(), library.getName(), library.getVersion()));
			}
		}
		request.setAttribute("dependencies", dependencies);
		DependencyBean newDependency = null;
		String newDependencyId = request.getParameter("newDependency");
		if (newDependencyId != null && !newDependencyId.isEmpty()) {
			try {
				File artifact = Repository.getDefaultInstance().getArtifact(newDependencyId);
				if (artifact != null) {
					String[] idSplit = newDependencyId.split(":");
					libraries.add(new Library(getNeoService(), idSplit[0], idSplit[1], idSplit[2]));
					changed = true;
					newDependency = new DependencyBean(idSplit[0], idSplit[1], idSplit[2]);
				}
			} catch (RepositoryException e) {
				request.setAttribute("errorMessage", "Unable to locate " + newDependencyId);
			}
		}
		System.out.println(libraries);

		if (transformation != null) {
			if (transformation instanceof JavaTransformation) {
				if (!((JavaTransformation) transformation).getSource().getCode().equals(source)) {
					// Update because the source is different
					changed = true;
				}
			} else {
				changed = true;
			}
			request.setAttribute("version", transformation.getVersion());
		} else {
			changed = true;
		}
		if (changed) {
			FileVersion newFile = new FileVersion(getNeoService(), source);
			try {
				transformation = JavaRuntimeFactory.getDefaultInstance().generateTransformation(transformationName,
						newFile, libraries);
				request.setAttribute("version", transformation.getVersion());
				if (newDependency != null) {
					dependencies.add(newDependency);
				}
				UserBean currentUser = (UserBean) request.getSession().getAttribute("user");
				factory.addTransformation(currentUser.getLogin(), transformation);
			} catch (TransformationException e) {
				if (!(e.getCause() instanceof CompilationFailedException)) {
					throw new ServletException(e);
				}
				List<Diagnostic<? extends JavaFileObject>> diagnostics = ((CompilationFailedException) e.getCause())
						.getDiagnostics();
				List<String> errorMessages = new ArrayList<String>();
				List<ErrorBean> errors = new ArrayList<ErrorBean>();
				for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
					long line = diagnostic.getLineNumber();
					long col = diagnostic.getColumnNumber();
					String message = diagnostic.getMessage(Locale.getDefault());
					errors.add(new ErrorBean(line, col, message));

					errorMessages.add(line + ":" + col + " - " + message);
				}
				request.setAttribute("errorMessages", errorMessages);
				request.setAttribute("errors", errors);
			}
		}

		if (action.equals("Save and Close")) {
			response.sendRedirect(request.getContextPath() + "/transformation/" + transformationPath);
		} else {
			RequestDispatcher rd = request.getRequestDispatcher("edit-transformation.jsp");
			rd.forward(request, response);
		}
	}
	
	public static class ErrorBean {
		private long line;
		private long col;
		private String message;

		public ErrorBean(long line, long col, String message) {
			this.line = line;
			this.col = col;
			this.message = message;
		}

		public long getLine() {
			return line;
		}

		public long getCol() {
			return col;
		}

		public String getMessage() {
			return message;
		}
	}
}
