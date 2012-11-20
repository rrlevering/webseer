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

import org.webseer.model.meta.InputPoint;
import org.webseer.model.meta.OutputPoint;
import org.webseer.model.meta.Transformation;
import org.webseer.model.meta.TransformationException;
import org.webseer.transformation.InputReaders;
import org.webseer.transformation.LanguageFactory;
import org.webseer.transformation.OutputWriters;
import org.webseer.transformation.PullRuntimeTransformation;
import org.webseer.transformation.RuntimeTransformationException;
import org.webseer.transformation.TransformationFactory;

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
		
		Transformation transformation = factory.getLatestTransformationByName(transformationName);
		
		request.setAttribute("transformation", transformation);

		RequestDispatcher rd = request.getRequestDispatcher("transformation.jsp");
		rd.forward(request, response);
	}

	
	@Override
	protected void transactionalizedPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String transformationPath = (String) getRequiredAttribute(request, "transformationPath");
		String transformationName = transformationPath.replace('/', '.');

		TransformationFactory factory = TransformationFactory.getTransformationFactory(getNeoService(), true);

		Transformation transformation = factory.getLatestTransformationByName(transformationName);

		Map<String, Object> inputs = new HashMap<String, Object>();
		
		PullRuntimeTransformation runtimeTransformation;
		try {
			runtimeTransformation = LanguageFactory.getInstance().generatePullTransformation(transformation);

			for (InputPoint input : transformation.getInputPoints()) {
				String serialized = request.getParameter(input.getName());
				// Convert to correct type from string representation
				Object inputObject = input.getType().fromString(serialized);
				inputs.put(input.getName(), inputObject);
				runtimeTransformation.addInputChannel(input.getName(), InputReaders.getInputReader(inputObject));
			}
		} catch (TransformationException e) {
			// This is an application error, there shouldn't be a mismatch here
			throw new IllegalStateException("Input points in runtime transformation should match transformation", e);
		}

		Map<String, List<Object>> outputs = new HashMap<String, List<Object>>();

		for (OutputPoint output : transformation.getOutputPoints()) {
			List<Object> collector = new ArrayList<Object>();
			runtimeTransformation.addOutputChannel(output.getName(), OutputWriters.getOutputWriter(collector));
			outputs.put(output.getName(), collector);
		}

		try {
			runtimeTransformation.transform();
		} catch (TransformationException e) {
			throw new IllegalStateException("Input points in runtime transformation should match transformation", e);
		} catch (RuntimeTransformationException e) {
			throw new ServletException("Problem running transformation", e);
		}

		request.setAttribute("transformation", transformation);
		request.setAttribute("outputs", outputs);
		request.setAttribute("inputs", inputs);

		RequestDispatcher rd = request.getRequestDispatcher("run-transformation.jsp");
		rd.forward(request, response);
	}
}
