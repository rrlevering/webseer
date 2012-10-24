package org.webseer.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.transformation.TransformationFactory;

import com.google.common.collect.Lists;

public class ListTransformations extends SeerServlet {
	
	/**
	 * Id for serializable servlet.
	 */
	private static final long serialVersionUID = 8392196460202025L;

	@Override
	protected void transactionalizedService(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		
		request.setAttribute("transformations", Lists.newArrayList(factory.getAllTransformations()));

		RequestDispatcher rd = request.getRequestDispatcher("transformations.jsp");
		rd.forward(request, response);
	}

}
