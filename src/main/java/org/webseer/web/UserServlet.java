package org.webseer.web;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.User;
import org.webseer.model.UserFactory;
import org.webseer.web.beans.UserBean;

public class UserServlet extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void transactionalizedService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		UserBean user = (UserBean) request.getSession().getAttribute("user");

		String action = request.getParameter("action");
		if (action != null) {
			GraphDatabaseService service = getNeoService();
			UserFactory factory = UserFactory.getUserFactory(service);
			if (action.equals("delete")) {
				// TODO: Not implemented yet
			} else if (action.equals("edit")) {
				// If we don't have a user id, we're creating
				String userId = request.getParameter("userid");
				if (userId == null) {
					String newUserId = request.getParameter("newUserId");
					if (newUserId == null || newUserId.isEmpty()) {
						request.setAttribute("error", "Invalid user id");

						RequestDispatcher rd = request.getRequestDispatcher("user.jsp");
						rd.forward(request, response);
						return;
					}
					// Check the user id
					if (factory.getUser(newUserId, service) != null) {
						request.setAttribute("error", "User id is not available");

						RequestDispatcher rd = request.getRequestDispatcher("user.jsp");
						rd.forward(request, response);
						return;
					}

					String email = request.getParameter("email");
					String name = request.getParameter("name");

					String password = request.getParameter("password");
					String passwordCheck = request.getParameter("passwordCheck");
					if (!password.equals(passwordCheck)) {
						request.setAttribute("error", "Passwords do not match");

						RequestDispatcher rd = request.getRequestDispatcher("user.jsp");
						rd.forward(request, response);
						return;
					}
					password = encode(password);

					User created = new User(service, factory, newUserId, password, name, email);

					request.setAttribute("error", "User created");
					request.getSession().setAttribute("user",
							new UserBean(created.getLogin(), created.getEmail(), created.getName()));
				} else {
					User dbUser = UserFactory.getUserFactory(service).getUser(user.getLogin(), service);

					String email = request.getParameter("email");
					String name = request.getParameter("name");

					String password = request.getParameter("password");
					String passwordCheck = request.getParameter("passwordCheck");
					if (password != null && !password.isEmpty()) {
						if (!password.equals(passwordCheck)) {
							request.setAttribute("error", "New passwords do not match");

							RequestDispatcher rd = request.getRequestDispatcher("user.jsp");
							rd.forward(request, response);
							return;
						}
						password = encode(password);
					} else {
						password = dbUser.getPassword();
					}
					String oldPassword = request.getParameter("oldPassword");
					oldPassword = encode(oldPassword);

					if (!dbUser.getPassword().equals(oldPassword)) {
						request.setAttribute("error", "Password is incorrect");

						RequestDispatcher rd = request.getRequestDispatcher("user.jsp");
						rd.forward(request, response);
						return;
					}

					dbUser.setPassword(password);
					dbUser.setEmail(email);
					dbUser.setName(name);

					request.setAttribute("error", "User updated");
					request.setAttribute("user", new UserBean(user.getLogin(), user.getEmail(), user.getName()));
				}
			}

		}

		RequestDispatcher rd = request.getRequestDispatcher("user.jsp");
		rd.forward(request, response);
	}

	static String encode(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");

			byte[] encoded = digest.digest(password.getBytes());

			return new String(encoded);
		} catch (NoSuchAlgorithmException e) {
			// Should not happen
			e.printStackTrace();
			return password;
		}
	}
}
