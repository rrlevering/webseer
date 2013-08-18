package org.webseer.web.beans;

import org.webseer.model.meta.Bucket;
import org.webseer.streams.model.WorkspaceBucket;

public class BucketBean {

	private String name;

	public BucketBean(Bucket bucket) {
		this.name = bucket.getName();
	}

	public BucketBean(WorkspaceBucket bucket) {
		this.name = bucket.getName();
	}

	public String getName() {
		return name;
	}

}
