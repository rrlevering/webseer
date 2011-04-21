package org.webseer.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.graphdb.GraphDatabaseService;
import org.webseer.model.Workspace;
import org.webseer.model.runtime.RuntimeConfigurationImpl;

public class WebConfigurationImpl extends RuntimeConfigurationImpl {

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final Boolean editRights;
	private final String servletPath;
	private final String bucketIO;

	public WebConfigurationImpl(HttpServletRequest request, HttpServletResponse response, final Workspace workspace,
			final String userName, String servletPath, GraphDatabaseService service, Boolean editRights, String bucketIO) {
		super(service, workspace, userName);
		this.request = request;
		this.response = response;
		this.servletPath = servletPath;
		this.editRights = editRights;
		this.bucketIO = bucketIO;
	}

}