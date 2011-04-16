package org.webseer.web;

import java.io.IOException;
import java.net.URLEncoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.web.beans.UserBean;

public class LoggedInFilter implements Filter {

	private String contextName = null;

	@Override
	public void destroy() {
		// Do nothing
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filters) throws IOException,
			ServletException {
		UserBean user = (UserBean) ((HttpServletRequest) req).getSession().getAttribute("user");

		if (user == null) {
			((HttpServletResponse) resp).sendRedirect(contextName + "/login?redirect="
					+ URLEncoder.encode(((HttpServletRequest) req).getRequestURI(), "UTF-8"));
			return;
		}

		filters.doFilter(req, resp);

	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		// Do nothing
		contextName = config.getServletContext().getContextPath();
	}

}
