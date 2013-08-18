package org.webseer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.java.JavaTransformation;
import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.InputReaders;
import org.webseer.transformation.OutputWriters;
import org.webseer.transformation.PullRuntimeTransformation;
import org.webseer.transformation.RuntimeTransformationException;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.beans.UserBean;

public class ViewTransformation extends SeerServlet {

	/**
	 * Id for serializable servlet.
	 */
	private static final long serialVersionUID = 2136535241297653889L;

	@Override
	protected void transactionalizedGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String transformationPath = (String) getRequiredAttribute(request, "transformationPath");
		String transformationName = transformationPath.replace('/', '.');
		
		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);
		
		JavaTransformation transformation = (JavaTransformation) factory.getLatestTransformationByName(transformationName);
		
		System.out.println("Got transformation " + transformation + " from " + factory);
		request.setAttribute("transformation", transformation);
		request.setAttribute("source", transformation.getSource());

		UserBean currentUser = (UserBean) request.getSession().getAttribute("user");
		request.setAttribute("editable", (currentUser != null && currentUser.getLogin().equals(transformation.getOwner())));
		
		RequestDispatcher rd = request.getRequestDispatcher("transformation.jsp");
		rd.forward(request, response);
	}
	
	@Override
	protected void transactionalizedPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String transformationPath = (String) getRequiredAttribute(request, "transformationPath");
		String transformationName = transformationPath.replace('/', '.');

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);

		JavaTransformation transformation = (JavaTransformation) factory.getLatestTransformationByName(transformationName);

		Map<String, String> inputs = new HashMap<String, String>();
		
		PullRuntimeTransformation runtimeTransformation;
		try {
			runtimeTransformation = transformation.getPullRuntimeTransformation();

			for (InputPoint input : transformation.getInputPoints()) {
				String serialized = request.getParameter(input.getName());
				inputs.put(input.getName(), serialized);
				runtimeTransformation.addInputChannel(input.getName(), InputReaders.getInputReader(serialized));
			}
		} catch (TransformationException e) {
			// This is an application error, there shouldn't be a mismatch here
			throw new IllegalStateException("Input points in runtime transformation should match transformation", e);
		}

		Map<String, List<String>> outputs = new HashMap<String, List<String>>();

		for (OutputPoint output : transformation.getOutputPoints()) {
			List<String> collector = new ArrayList<String>();
			runtimeTransformation.addOutputChannel(output.getName(), OutputWriters.getStringWriter(collector));
			outputs.put(output.getName(), collector);
		}

		try {
			runtimeTransformation.transform();
		} catch (TransformationException e) {
			throw new IllegalStateException("Input points in runtime transformation should match transformation", e);
		} catch (RuntimeTransformationException e) {
			//TODO: handle this in the output JSP
			throw new ServletException("Problem running transformation", e);
		}

		request.setAttribute("transformation", transformation);
		request.setAttribute("source", transformation.getSource());
		request.setAttribute("outputs", outputs);
		request.setAttribute("inputs", inputs);

		RequestDispatcher rd = request.getRequestDispatcher("run-transformation.jsp");
		rd.forward(request, response);
	}
}
