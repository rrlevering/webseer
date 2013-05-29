package org.webseer.web.ajax;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.webseer.java.JavaRuntimeFactory;
import org.webseer.java.JavaRuntimeFactory.CompilationFailedException;
import org.webseer.java.JavaTransformation;
import org.webseer.model.meta.FileVersion;
import org.webseer.model.meta.Library;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.SeerServlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class CompileTransformation extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		String name = getRequiredParameter(req, "name");
		String source = getRequiredParameter(req, "source");
		
		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		Transformation transformation = factory.getLatestTransformationByName(name);
		
		JsonObject response = new JsonObject();

		List<Library> libraries = new ArrayList<Library>();
		if (transformation instanceof JavaTransformation) {
			for (Library library : ((JavaTransformation) transformation).getLibraries()) {
				libraries.add(library);
			}
		}

		FileVersion newFile = new FileVersion(getNeoService(), source);
		try {
			JavaRuntimeFactory.getDefaultInstance().generateTransformation(name, newFile, libraries);
		} catch (TransformationException e) {
			if (!(e.getCause() instanceof CompilationFailedException)) {
				throw new ServletException(e);
			}
			List<Diagnostic<? extends JavaFileObject>> diagnostics = ((CompilationFailedException) e.getCause())
					.getDiagnostics();
			JsonArray errors = new JsonArray();

			for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
				long line = diagnostic.getLineNumber();
				long col = diagnostic.getColumnNumber();
				String message = diagnostic.getMessage(Locale.getDefault());
				JsonObject error = new JsonObject();
				error.add("reason", new JsonPrimitive(message));
				error.add("line", new JsonPrimitive(line));
				error.add("col", new JsonPrimitive(col));

				errors.add(error);
			}

			response.add("errors", errors);

		}
		Writer writer = resp.getWriter();
		writer.write(response.toString());
		writer.close();

	}

}
