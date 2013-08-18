package org.webseer.bucket;

import java.io.InputStream;

import org.webseer.model.meta.Field;
import org.webseer.type.DataType;

/**
 * An interface to represent a piece of typed data.
 */
public interface Data {

	/**
	 * Gets the type of this piece of data.
	 */
	public DataType getType();

	/**
	 * Gets the data represented by a field of this data object. If the field does not exist, returns null.
	 */
	public Data getField(Field field);

	/**
	 * Gets the value of this piece of data as a raw byte stream. This stream should be interpretable via the type into
	 * a target language.
	 */
	public InputStream getValue();

}
