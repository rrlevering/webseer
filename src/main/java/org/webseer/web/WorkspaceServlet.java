package org.webseer.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.webseer.model.Neo4JUtils;
import org.webseer.streams.model.Workspace;

public abstract class WorkspaceServlet extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void nontransactionalizedGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String workspaceId = (String) getRequiredAttribute(req, "workspaceId");

		Workspace workspace;
		GraphDatabaseService service = getNeoService();
		Transaction tran = service.beginTx();
		try {
			workspace = Neo4JUtils.get(service, Long.parseLong(workspaceId), Workspace.class);
			tran.success();
		} finally {
			tran.finish();
		}

		serviceWorkspace(workspace, req, resp);
	}

	protected void serviceWorkspace(Workspace workspace, HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		GraphDatabaseService service = getNeoService();
		Transaction tran = service.beginTx();
		try {
			transactionalizedServiceWorkspace(workspace, req, resp);
			tran.success();
		} finally {
			tran.finish();
		}
	}

	protected void transactionalizedServiceWorkspace(Workspace workspace, HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		// Do nothing by default
	}

}
