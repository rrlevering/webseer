package org.webseer.transformation;

import java.util.Iterator;

import org.webseer.bucket.Data;

public interface InputReader extends Iterable<Data> {
	
	public Iterator<Data> iterator();

}
