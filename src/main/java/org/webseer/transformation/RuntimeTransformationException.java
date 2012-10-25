package org.webseer.transformation;

public class RuntimeTransformationException extends Exception {

	private static final long serialVersionUID = 1L;

	public RuntimeTransformationException(Throwable cause) {
		super(cause);
	}

	public RuntimeTransformationException(String string, Throwable cause) {
		super(string, cause);
	}
}
