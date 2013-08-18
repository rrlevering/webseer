package org.webseer.bucket;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.webseer.model.Neo4JUtils;
import org.webseer.model.meta.Bucket;
import org.webseer.transformation.InputReader;
import org.webseer.transformation.OutputWriter;
import org.webseer.type.SimpleType;
import org.webseer.util.WebseerConfiguration;

/**
 * Implementation of a write-once file bucket.
 */
public class FileBucket extends Bucket {

	private final File file;

	public FileBucket(GraphDatabaseService service, String bucketPath) {
		super(Neo4JUtils.createNode(service, FileBucket.class), bucketPath);
		File baseDir = new File(WebseerConfiguration.getWebseerRoot(), WebseerConfiguration.getConfiguration()
				.getString("WEBSEER_BUCKET_DIR"));
		this.file = new File(baseDir, bucketPath);
	}

	public FileBucket(Node underlyingNode) {
		super(underlyingNode);
		File baseDir = new File(WebseerConfiguration.getWebseerRoot(), WebseerConfiguration.getConfiguration()
				.getString("WEBSEER_BUCKET_DIR"));
		this.file = new File(baseDir, getName());
	}

	@Override
	public InputReader getBucketReader() {
		return new FileReader();
	}

	@Override
	public OutputWriter getBucketWriter() {
		return new FileWriter();
	}

	public class FileReader implements InputReader {

		private final BufferedInputStream reader;

		FileReader() {
			try {
				reader = new BufferedInputStream(new java.io.FileInputStream(file));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Iterator<Data> iterator() {
			return new Iterator<Data>() {
				@Override
				public boolean hasNext() {
					reader.mark(1);
					int data;
					try {
						data = reader.read();
						reader.reset();
					} catch (IOException e) {
						return false;
					}
					if (data < 0) {
						return false;
					}
					return true;
				}

				@Override
				public Data next() {
					DataInputStream dataInput = new DataInputStream(reader);
					String name;
					byte[] dataBytes;
					try {
						name = dataInput.readUTF();
						int length = dataInput.readInt();

						dataBytes = new byte[length];
						dataInput.read(dataBytes);
					} catch (IOException e) {
						throw new RuntimeException("Problem decoding data", e);
					}

					return new SimpleData(dataBytes, new SimpleType(name));
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

			};
		}

	}

	public class FileWriter implements OutputWriter {

		private java.io.FileOutputStream writer;

		FileWriter() {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			}
			try {
				writer = new java.io.FileOutputStream(file, true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void writeData(Data item) {
			DataOutputStream dataOutput = new DataOutputStream(writer);
			try {
				dataOutput.writeUTF(item.getType().getName());

				byte[] bytes = IOUtils.toByteArray(item.getValue());
				dataOutput.writeInt(bytes.length);
				dataOutput.write(bytes);
				dataOutput.flush();

			} catch (IOException e) {
				throw new RuntimeException("Problem writing data", e);
			}
		}

	}

}
