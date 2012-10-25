package org.webseer.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.meta.TransformationException;
import org.webseer.streams.model.Workspace;
import org.webseer.streams.model.program.TransformationGraph;
import org.webseer.streams.model.program.TransformationNode;
import org.webseer.streams.model.runtime.BucketReader;
import org.webseer.streams.model.runtime.RuntimeConfiguration;
import org.webseer.streams.model.trace.Item;

import com.google.protobuf.ByteString;

public class RenderNode extends WorkspaceServlet {

	private static final long serialVersionUID = 1L;

	protected void transactionalizedServiceWorkspace(Workspace workspace, HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		String nodeIdString = req.getParameter("nodeId");
		int nodeId = Integer.parseInt(nodeIdString);
		TransformationNode node = Neo4JUtils.get(getNeoService(), nodeId, TransformationNode.class);

		GraphDatabaseService service = getNeoService();

		RuntimeConfiguration config = new WebConfigurationImpl(req, resp, workspace, getCurrentUser(req).getLogin(),
				getServletContext().getRealPath(""), service, isUser(workspace.getOwner(), req), null);

		TransformationNode cloned = TransformationGraph.createRuntimeGraph(service, node, new HashMap<Node, Node>());

		try {
			BucketReader[] items = config.getBucketReaders(cloned, new String[] { "fileName", "contentType",
					"httpResponse" });

			// Just read the first thing from each bucket
			Item item = items[1].getItems().next();

			String contentType = (String) item.get();
			if (items[0] != null) {
				String fileName = (String) items[0].getItems().next().get();
				resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
			}
			ByteString response = (ByteString) items[2].getItems().next().get();

			resp.setContentType(contentType);

			OutputStream stream = resp.getOutputStream();
			stream.write(response.toByteArray());
			stream.close();

			// This should delete everything since it's not referenced
			item.getOutputGroup().checkForDelete();
		} catch (TransformationException e) {
			throw new ServletException(e);
		}
	}

}
