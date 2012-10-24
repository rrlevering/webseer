package org.webseer.web.beans;

import org.webseer.streams.model.WorkspaceBucket;

public class BucketBean {

	private String name;

	private long id;

	private String type;

	public BucketBean(WorkspaceBucket bucket) {
		this.id = bucket.getBucketId();
		this.name = bucket.getName();
		if (bucket.getType() != null) {
			this.type = bucket.getType().getName();
		}
	}

	public String getName() {
		return name;
	}

	public long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

}
