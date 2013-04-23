package org.webseer.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String CLIENT_ID = "651154329698.apps.googleusercontent.com";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String state;
		if (request.getParameter("redirect") != null) {
			state = request.getParameter("redirect");
		} else {
			state = "index";
		}

		response.sendRedirect("https://accounts.google.com/o/oauth2/auth?response_type=code&client_id="
				+ CLIENT_ID
				+ "&state="
				+ state
				+ "&scope=https://www.googleapis.com/auth/userinfo.email+https://www.googleapis.com/auth/userinfo.profile"
				+ "&redirect_uri=https://localhost:8443" + this.getServletContext().getContextPath() + "/googleSignin");
	}
}
