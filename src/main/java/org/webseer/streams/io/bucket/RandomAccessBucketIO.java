package org.webseer.streams.io.bucket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import name.levering.ryan.util.Pair;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.webseer.streams.io.BucketIO;
import org.webseer.streams.io.ModelIO;
import org.webseer.util.WebseerConfiguration;

public class RandomAccessBucketIO implements BucketIO {

	private static final String WORKING_DIR = "bucketData";

	private final File workingDir;

	private final Map<Long, RandomAccessFile> cache = new HashMap<Long, RandomAccessFile>();

	public RandomAccessBucketIO() {
		this.workingDir = new File(WebseerConfiguration.getWebseerRoot(), WORKING_DIR);
		this.workingDir.mkdirs();
	}

	private RandomAccessFile getFile(long bucketId) {
		RandomAccessFile file = cache.get(bucketId);
		if (file == null) {
			try {
				file = new RandomAccessFile(new File(workingDir, bucketId + ".data"), "rw");
			} catch (FileNotFoundException e) {
				return null;
			}
			cache.put(bucketId, file);
		}
		return file;
	}

	public <T> T getItem(long bucketId, long offset, ModelIO<T> factory) {
		RandomAccessFile dataFile = getFile(bucketId);
		if (dataFile == null) {
			return null;
		}
		try {
			dataFile.seek(offset);
		} catch (IOException e) {
			return null;
		}

		return factory.read(new RandomAccessFileInputStream(dataFile));
	}

	public <T> long putItem(long bucketId, T model, ModelIO<T> factory, Long currentOffset) {
		RandomAccessFile dataFile = getFile(bucketId);

		long offset;
		try {
			if (currentOffset != null) {
				offset = currentOffset;
			} else {
				offset = dataFile.length();
			}
			dataFile.seek(offset);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		factory.write(new RandomAccessFileOutputStream(dataFile), model);

		return offset;
	}

	public <T> Iterable<T> getItems(final long bucketId, final ModelIO<T> factory) {

		return new Iterable<T>() {

			@SuppressWarnings("unchecked")
			public Iterator<T> iterator() {
				File bucketData = new File(workingDir, bucketId + ".data");
				final RandomAccessFile input;
				final RandomAccessFileInputStream stream;
				try {
					input = new RandomAccessFile(bucketData, "r");
					stream = new RandomAccessFileInputStream(input);
				} catch (FileNotFoundException e) {
					return IteratorUtils.emptyIterator();
				}
				return new Iterator<T>() {

					public boolean hasNext() {
						boolean hasNext;
						try {
							hasNext = input.getFilePointer() < input.length();
						} catch (IOException e) {
							return false;
						}
						if (!hasNext) {
							try {
								input.close();
							} catch (IOException e) {
								// Ignore
							}
						}
						return hasNext;
					}

					public T next() {
						return factory.read(stream);
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}

				};
			}
		};
	}

	public static class RandomAccessFileInputStream extends InputStream {

		private final RandomAccessFile file;

		public RandomAccessFileInputStream(RandomAccessFile file) {
			this.file = file;
		}

		@Override
		public int read() throws IOException {
			return this.file.read();
		}

		@Override
		public int read(byte[] bytes, int offset, int len) throws IOException {
			return this.file.read(bytes, offset, len);
		}

	}

	public static class RandomAccessFileOutputStream extends OutputStream {

		private final RandomAccessFile file;

		public RandomAccessFileOutputStream(RandomAccessFile file) {
			this.file = file;
		}

		@Override
		public void write(int b) throws IOException {
			file.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			file.write(b, off, len);
		}

	}

	@Override
	public Pair<Long, OutputStream> getOutputStream(long bucketId, Long currentOffset) {
		RandomAccessFile dataFile = getFile(bucketId);

		long offset;
		try {
			if (currentOffset != null) {
				offset = currentOffset;
			} else {
				offset = dataFile.length();
			}
			dataFile.seek(offset);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new Pair<Long, OutputStream>(offset, new RandomAccessFileOutputStream(dataFile));
	}

	@Override
	public InputStream getInputStream(long bucketId, long offset) {
		RandomAccessFile dataFile = getFile(bucketId);

		try {
			dataFile.seek(offset);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new RandomAccessFileInputStream(dataFile);
	}

	public void copyBucketData(long id, long copyId) {
		File source = new File(workingDir, id + ".data");
		if (source.exists()) {
			File target = new File(workingDir, copyId + ".data");

			try {
				FileUtils.copyFile(source, target);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void deleteBucket(long bucketId) {
		File source = new File(workingDir, bucketId + ".data");
		if (source.exists()) {
			source.delete();
		}
	}
}
