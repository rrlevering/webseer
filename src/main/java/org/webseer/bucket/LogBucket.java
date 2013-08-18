package org.webseer.bucket;

import org.webseer.transformation.InputReader;
import org.webseer.transformation.OutputWriter;

/**
 * A bucket of typed data
 */
public interface LogBucket {

	/**
	 * Gets a reader that will read from the beginning on the stream.
	 */
	public InputReader getBucketReader();
	
	/**
	 * Returns a writer that will write to the end of the stream.
	 */
	public OutputWriter getBucketWriter();

}
