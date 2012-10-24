package org.webseer.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.webseer.model.Neo4JUtils;
import org.webseer.streams.model.Workspace;
import org.webseer.streams.model.program.TransformationNode;
import org.webseer.streams.model.program.TransformationNodeInput;

public class UploadFileToWorkspace extends WorkspaceServlet {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	@Override
	protected void transactionalizedServiceWorkspace(Workspace workspace, HttpServletRequest req,
			HttpServletResponse resp) throws ServletException, IOException {
		// Create a factory for disk-based file items
		FileItemFactory uploadFactory = new DiskFileItemFactory();

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(uploadFactory);

		// Parse the request
		List<FileItem> items;
		try {
			items = upload.parseRequest(req);
		} catch (FileUploadException e) {
			throw new ServletException("Problems with uploading the file", e);
		}

		int nodeId = -1;
		String inputName = null;
		String fileName = null;

		InputStream stream = null;

		for (FileItem item : items) {
			if (item.isFormField()) {
				String name = item.getFieldName();
				String value = item.getString();

				if (name.equals("nodeId")) {
					nodeId = Integer.parseInt(value);
				} else if (name.equals("inputName")) {
					inputName = value;
				}
			} else {
				// String fieldName = item.getFieldName();
				fileName = item.getName();
				// String contentType = item.getContentType();
				// boolean isInMemory = item.isInMemory();
				// long sizeInBytes = item.getSize();

				// Write the byte strings
				stream = item.getInputStream();
			}
		}

		TransformationNode node = Neo4JUtils.get(getNeoService(), nodeId, TransformationNode.class);

		TransformationNodeInput input = node.getInput(inputName);

		OutputStream output = input.getOutputStream();
		IOUtils.copy(stream, output);
		output.close();

		input.setMeta(fileName);

		// Generate a json response
		Writer writer = resp.getWriter();
		writer.append(fileName);
		writer.close();
	}
}
