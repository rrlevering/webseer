package org.webseer.web;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.webseer.model.User;
import org.webseer.util.WebseerConfiguration;
import org.webseer.web.beans.UserBean;

public abstract class SeerServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(SeerServlet.class.getName());

	private static final long serialVersionUID = 1L;

	public GraphDatabaseService getNeoService() {
		GraphDatabaseService service = (GraphDatabaseService) getServletContext().getAttribute("neoService");
		if (service == null) {
			String dbDir = new File(WebseerConfiguration.getWebseerRoot(), WebseerConfiguration.getConfiguration()
					.getString("WEBSEER_DB_DIR")).getAbsolutePath();
			service = new EmbeddedGraphDatabase(dbDir);
			getServletContext().setAttribute("neoService", service);
			log.info("Starting webseer with db at " + dbDir);
		}
		return service;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		nontransactionalizedGet(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		nontransactionalizedPost(req, resp);
	}

	protected String getRequiredParameter(HttpServletRequest req, String parameterName) throws ServletException {
		String parameter = req.getParameter(parameterName);
		if (parameter == null) {
			throw new ServletException("Expected parameter '" + parameterName + "'");
		}
		return parameter;
	}

	protected Object getRequiredAttribute(HttpServletRequest req, String parameterName) throws ServletException {
		Object parameter = req.getAttribute(parameterName);
		if (parameter == null) {
			throw new ServletException("Expected attribute '" + parameterName + "'");
		}
		return parameter;
	}

	protected UserBean getCurrentUser(HttpServletRequest req) {
		return (UserBean) req.getSession().getAttribute("user");
	}

	protected boolean isUser(User user, HttpServletRequest req) {
		UserBean currentUser = getCurrentUser(req);
		return currentUser != null && currentUser.getLogin().equals(user.getLogin());
	}

	protected void nontransactionalizedGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		GraphDatabaseService service = getNeoService();
		Transaction tran = service.beginTx();
		try {
			transactionalizedGet(req, resp);
			tran.success();
		} finally {
			tran.finish();
		}
	}

	protected void nontransactionalizedPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		GraphDatabaseService service = getNeoService();
		Transaction tran = service.beginTx();
		try {
			transactionalizedPost(req, resp);
			tran.success();
		} finally {
			tran.finish();
		}
	}

	protected void transactionalizedGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		transactionalizedService(req, resp);
	}

	protected void transactionalizedPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		transactionalizedService(req, resp);
	}

	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		// Do nothing, override if desired
	}

}
