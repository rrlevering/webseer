package org.webseer.model.meta;

/**
 * Generic transformation exception that is used to represent an exception that occurred within the program level.
 * Otherwise, something happened while building the program that isn't allowed (the transformation doesn't exist
 * anymore, the connections are invalid for some reason).
 * 
 * @author ryan
 */
public class TransformationException extends Exception {

	private static final long serialVersionUID = 1L;

	public TransformationException(String string) {
		super(string);
	}

	public TransformationException(String string, Throwable cause) {
		super(string, cause);
	}

	public TransformationException(Throwable cause) {
		super(cause);
	}

}
