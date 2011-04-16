package org.webseer.io.bucket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import name.levering.ryan.util.BiMap;
import name.levering.ryan.util.HashBiMap;
import name.levering.ryan.util.Pair;

import org.webseer.io.BucketIO;
import org.webseer.io.ModelIO;

public class TransientMemoryIO implements BucketIO {

	private static BiMap<Long, Long, byte[]> buckets = new HashBiMap<Long, Long, byte[]>();

	public <T> T getItem(long bucketId, long index, ModelIO<T> factory) {
		ByteArrayInputStream input = new ByteArrayInputStream(buckets.get(bucketId, index));
		return factory.read(input);
	}

	public <T> long putItem(long bucketId, T model, ModelIO<T> factory, Long currentOffset) {
		Map<Long, byte[]> internal = buckets.asMap().get(bucketId);
		long index = internal == null ? 0 : internal.size();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		factory.write(output, model);
		buckets.put(bucketId, index, output.toByteArray());

		return index;
	}

	public long size(long bucketId) {
		return buckets.asMap().get(bucketId).size();
	}

	@Override
	public Pair<Long, OutputStream> getOutputStream(long bucketId, Long currentOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getInputStream(long bucketId, long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void copyBucketData(long bucketId, long newBucketId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteBucket(long bucketId) {
		// TODO Auto-generated method stub

	}

}
