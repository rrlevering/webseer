package org.webseer.streams.model.trace;

import java.io.InputStream;

import org.webseer.model.meta.Type;

public interface Data {

	Type getType();

	Type getType(String field);

	Object get();

	Object get(String field);

	InputStream getInputStream();

}
