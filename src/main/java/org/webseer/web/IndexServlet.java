package org.webseer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.meta.Transformation;
import org.webseer.transformation.TransformationFactory;
import org.webseer.web.beans.TransformationBean;
import org.webseer.web.beans.UserBean;

public class IndexServlet extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void transactionalizedService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		GraphDatabaseService service = getNeoService();
		TransformationFactory factory = TransformationFactory.getTransformationFactory(service);

		UserBean currentUser = (UserBean) request.getSession().getAttribute("user");
		Iterable<Transformation> transformations = factory.getAllTransformations();
		List<TransformationBean> publicTransformations = new ArrayList<TransformationBean>();
		List<TransformationBean> ownedTransformations = new ArrayList<TransformationBean>();
		
		for (Transformation transformation : transformations) {
			if (currentUser != null && currentUser.getLogin().equals(transformation.getOwner())) {
				ownedTransformations.add(new TransformationBean(transformation));
			} else if (transformation.getOwner() == null) {
				publicTransformations.add(new TransformationBean(transformation));
			}
		}
		request.setAttribute("ownedTransformations", ownedTransformations);
		request.setAttribute("publicTransformations", publicTransformations);

		String newTransformationName;
		if (currentUser != null) {
			newTransformationName = sanitize(currentUser.getEmail()) + "Test";
		} else {
			newTransformationName = "test";
		}
		int i = 1;
		String testName = newTransformationName;
		while (factory.getLatestTransformationByName(testName) != null) {
			testName = newTransformationName + i++;
		}
		request.setAttribute("newTransformationName", testName);
		
		RequestDispatcher rd = request.getRequestDispatcher("index.jsp");
		rd.forward(request, response);
	}
	
	private String sanitize(String email) {
		return email.substring(0, email.indexOf("@")).replaceAll("\\.", "_");
	}
}
