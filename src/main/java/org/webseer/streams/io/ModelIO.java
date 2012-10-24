package org.webseer.streams.io;

import java.io.InputStream;
import java.io.OutputStream;

public interface ModelIO<T> {

	public T read(InputStream stream);

	public void write(OutputStream stream, T toWrite);

	public String getSummary(T item);

	public String getDetails(T item);

}
