package org.webseer.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webseer.model.User;
import org.webseer.model.UserFactory;
import org.webseer.model.Workspace;
import org.webseer.model.WorkspaceFactory;
import org.webseer.web.beans.UserBean;
import org.webseer.web.beans.WorkspaceBean;

import com.google.gson.Gson;

public class CreateWorkspace extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		UserBean user = getCurrentUser(req);
		if (user == null) {
			throw new ServletException("Must be logged in to create a workspace");
		}
		String workspaceName = getRequiredParameter(req, "workspaceName");
		User owner = UserFactory.getUserFactory(getNeoService()).getUser(user.getLogin(), getNeoService());

		WorkspaceFactory factory = WorkspaceFactory.getWorkspaceFactory(getNeoService());
		Workspace newWorkspace = new Workspace(getNeoService(), factory, owner, workspaceName);

		// Return the new bean
		WorkspaceBean bean = new WorkspaceBean(newWorkspace);

		resp.setContentType("application/json");
		PrintWriter output = resp.getWriter();
		Gson gson = new Gson();
		String test = gson.toJson(bean);
		output.append(test);
	}
}
