package org.webseer.web;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.User;
import org.webseer.model.UserFactory;

public class PasswordResetServlet extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void transactionalizedService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		GraphDatabaseService service = getNeoService();

		if (request.getParameter("userid") != null) {
			String userid = request.getParameter("userid");

			UserFactory factory = UserFactory.getUserFactory(service);
			User user = factory.getUser(userid, service);

			if (user != null) {
				String generatedPassword = String.valueOf(new Random().nextLong());
				user.setPassword(UserServlet.encode(generatedPassword));

				// TODO: Send out email
				Properties props = new Properties();
				props.put("mail.smtp.host", "myHost");
				props.put("mail.smtp.auth", "true");

				response.sendRedirect("login");
				return;
			} else {
				// Wrong authentication
				request.setAttribute("error", "Invalid username/password");
			}
		}

		// Do nothing
		RequestDispatcher rd = request.getRequestDispatcher("password-reset.jsp");
		rd.forward(request, response);

	}
}
