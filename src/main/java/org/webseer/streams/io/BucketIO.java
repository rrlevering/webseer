package org.webseer.streams.io;

import java.io.InputStream;
import java.io.OutputStream;

import name.levering.ryan.util.Pair;

/**
 * A bucket has to allow random access to support walk-backs.
 */
public interface BucketIO {

	public <T> T getItem(long bucketId, long index, ModelIO<T> factory);

	public <T> long putItem(long bucketId, T model, ModelIO<T> modelIO, Long currentOffset);

	public Pair<Long, OutputStream> getOutputStream(long bucketId, Long currentOffset);

	public InputStream getInputStream(long bucketId, long offset);

	public void copyBucketData(long bucketId, long newBucketId);

	public void deleteBucket(long bucketId);

}
