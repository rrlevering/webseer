package org.webseer.model.runtime;

import java.lang.reflect.InvocationTargetException;

public class RuntimeTransformationException extends Exception {

	private static final long serialVersionUID = 1L;

	public RuntimeTransformationException(Throwable cause) {
		super(cause);
	}

	public RuntimeTransformationException(String string, InvocationTargetException e) {
		super(string, e);
	}
}
