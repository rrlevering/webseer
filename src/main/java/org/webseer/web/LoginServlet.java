package org.webseer.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.User;
import org.webseer.model.UserFactory;
import org.webseer.web.beans.UserBean;

public class LoginServlet extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void transactionalizedService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (request.getParameter("userid") != null) {
			String userid = request.getParameter("userid");
			String password = request.getParameter("password");
			GraphDatabaseService service = getNeoService();

			UserFactory factory = UserFactory.getUserFactory(service);
			User user = factory.getUser(userid, UserServlet.encode(password), service);

			if (user != null) {
				request.getSession().setAttribute("user",
						new UserBean(user.getLogin(), user.getEmail(), user.getName()));
				if (request.getParameter("redirect") != null) {
					response.sendRedirect(request.getParameter("redirect"));
				} else {
					response.sendRedirect("index");
				}
				return;
			} else {
				// Wrong authentication
				request.setAttribute("error", "Invalid username/password");
			}
		}

		// Do nothing
		RequestDispatcher rd = request.getRequestDispatcher("login.jsp");
		rd.forward(request, response);

	}
}
