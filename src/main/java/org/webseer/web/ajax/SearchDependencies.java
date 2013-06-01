package org.webseer.web.ajax;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.archiva.maven2.model.Artifact;
import org.webseer.repository.Repository;
import org.webseer.web.SeerServlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class SearchDependencies extends SeerServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void transactionalizedService(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		String query = getRequiredParameter(req, "q");
		
		Repository repo = Repository.getDefaultInstance();
		
		List<Artifact> localArtifacts = repo.searchLocalArtifacts(query);
		List<Artifact> remoteArtifacts = repo.searchCentral(query);
		
		JsonObject response = new JsonObject();
		
		JsonArray responseArtifacts = new JsonArray();
		
		Set<String> artifactsSet = new HashSet<>();

		for (Artifact artifact : localArtifacts) {
			String id = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
			if (artifactsSet.contains(artifact.getGroupId() + ":" + artifact.getArtifactId())) {
				continue;
			}
			String safeId = (artifact.getGroupId() + artifact.getArtifactId() + artifact.getVersion()).replaceAll("\\.", "_");
			artifactsSet.add(artifact.getGroupId() + ":" + artifact.getArtifactId());
			JsonObject artifactObject = new JsonObject();
			artifactObject.add("id", new JsonPrimitive(id));
			artifactObject.add("safeId", new JsonPrimitive(safeId));
			responseArtifacts.add(artifactObject);
		}
		for (Artifact artifact : remoteArtifacts) {
			String id = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
			if (artifactsSet.contains(artifact.getGroupId() + ":" + artifact.getArtifactId())) {
				continue;
			}
			String safeId = (artifact.getGroupId() + artifact.getArtifactId() + artifact.getVersion()).replaceAll("\\.", "_");
			artifactsSet.add(artifact.getGroupId() + ":" + artifact.getArtifactId());
			JsonObject artifactObject = new JsonObject();
			artifactObject.add("id", new JsonPrimitive(id));
			artifactObject.add("safeId", new JsonPrimitive(safeId));
			responseArtifacts.add(artifactObject);
		}
		System.out.println(query);
		System.out.println(responseArtifacts);

		response.add("dependencies", responseArtifacts);
		
		Writer writer = resp.getWriter();
		writer.write(response.toString());
		writer.close();

	}

}
