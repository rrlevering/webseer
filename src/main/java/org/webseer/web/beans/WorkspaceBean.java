package org.webseer.web.beans;

import name.levering.ryan.util.IterableUtils;

import org.webseer.model.Workspace;

/**
 * This bean is used whenever a summary of a workspace is needed without needing every group in the workspace to be
 * listed.
 * 
 * @author Ryan Levering
 */
public class WorkspaceBean {

	private final String name;

	private final String ownerName;

	private final long id;

	private final int bucketCount;

	private final int programCount;

	private final boolean publik;

	public WorkspaceBean(Workspace workspace) {
		this(workspace.getName(), workspace.getOwner().getName(), workspace.getWorkspaceId(), IterableUtils
				.size(workspace.getPrograms()), IterableUtils.size(workspace.getWorkspaceBuckets()), workspace
				.isPublic());
	}

	public WorkspaceBean(String name, String ownerName, long l, int programCount, int bucketCount, boolean publik) {
		this.name = name;
		this.ownerName = ownerName;
		this.id = l;
		this.bucketCount = bucketCount;
		this.programCount = programCount;
		this.publik = publik;
	}

	public String getName() {
		return this.name;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public long getId() {
		return this.id;
	}

	public int getBucketCount() {
		return bucketCount;
	}

	public int getProgramCount() {
		return programCount;
	}

	public boolean isPublic() {
		return publik;
	}
}
