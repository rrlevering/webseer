package org.webseer.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.model.meta.Transformation;
import org.webseer.transformation.TransformationFactory;

public class ViewTransformation extends SeerServlet {

	/**
	 * Id for serializable servlet.
	 */
	private static final long serialVersionUID = 2136535241297653889L;

	@Override
	protected void transactionalizedService(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String transformationPath = (String) getRequiredAttribute(request, "transformationPath");
		String transformationName = transformationPath.replace('/', '.');
		
		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		
		Transformation transformation = factory.getLatestTransformationByName(transformationName);
		
		request.setAttribute("transformation", transformation);

		RequestDispatcher rd = request.getRequestDispatcher("transformation.jsp");
		rd.forward(request, response);
	}

	
}
