package org.webseer.web;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.streams.model.Workspace;
import org.webseer.web.beans.WorkspaceBean;

public class EditWorkspace extends WorkspaceServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedServiceWorkspace(Workspace workspace, HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {

		req.setAttribute("workspace",
				new WorkspaceBean(workspace.getName(), workspace.getOwner().getName(), workspace.getWorkspaceId(), 0,
						0, workspace.isPublic()));

		RequestDispatcher rd = req.getRequestDispatcher("edit-workspace.jsp");
		rd.forward(req, resp);
	}

}
