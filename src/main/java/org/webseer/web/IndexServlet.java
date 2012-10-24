package org.webseer.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.streams.model.Workspace;
import org.webseer.streams.model.WorkspaceFactory;
import org.webseer.web.beans.UserBean;
import org.webseer.web.beans.WorkspaceBean;

public class IndexServlet extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void transactionalizedService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		GraphDatabaseService service = getNeoService();
		WorkspaceFactory factory = WorkspaceFactory.getWorkspaceFactory(service);

		UserBean currentUser = (UserBean) request.getSession().getAttribute("user");
		Iterable<Workspace> workspaces = factory.getAllWorkspaces();
		List<WorkspaceBean> publicWorkspaces = new ArrayList<WorkspaceBean>();
		List<WorkspaceBean> ownedWorkspaces = new ArrayList<WorkspaceBean>();
		for (Workspace workspace : workspaces) {
			if (currentUser != null && workspace.getOwner().getLogin().equals(currentUser.getLogin())) {
				ownedWorkspaces.add(new WorkspaceBean(workspace));
			} else if (workspace.isPublic()) {
				publicWorkspaces.add(new WorkspaceBean(workspace));
			}
		}
		request.setAttribute("ownedWorkspaces", ownedWorkspaces);
		request.setAttribute("publicWorkspaces", publicWorkspaces);

		RequestDispatcher rd = request.getRequestDispatcher("index.jsp");
		rd.forward(request, response);
	}
}
