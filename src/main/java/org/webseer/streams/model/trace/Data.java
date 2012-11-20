package org.webseer.streams.model.trace;

import java.io.InputStream;

import org.webseer.model.meta.UserType;

public interface Data {

	UserType getType();

	UserType getType(String field);

	Object get();

	Object get(String field);

	InputStream getInputStream();

}
