package org.webseer.streams.model.trace;

public interface HasValue {

	static final String VALUE = "VALUE";

	static final String BUCKET_INDEX = "bucketIndex";

	static final String BUCKET_OFFSET = "bucketOffset";

	public void setValue(Object value);

	public Object getValue();

	public Long getOffset();

	public long getBucketId();

	public void setOffset(long first);

}
