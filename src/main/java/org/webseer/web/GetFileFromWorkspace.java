package org.webseer.web;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.webseer.model.Neo4JUtils;
import org.webseer.streams.model.Workspace;
import org.webseer.streams.model.program.TransformationNode;
import org.webseer.streams.model.program.TransformationNodeInput;

public class GetFileFromWorkspace extends WorkspaceServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedServiceWorkspace(Workspace workspace, HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		String nodeIdString = req.getParameter("nodeId");
		String inputName = req.getParameter("inputName");
		int nodeId = Integer.parseInt(nodeIdString);

		TransformationNode node = Neo4JUtils.get(getNeoService(), nodeId, TransformationNode.class);

		TransformationNodeInput input = node.getInput(inputName);

		String fileName = input.getMeta();
		InputStream inputStream = input.getInputStream();

		resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
		IOUtils.copy(inputStream, resp.getOutputStream());
	}

}
