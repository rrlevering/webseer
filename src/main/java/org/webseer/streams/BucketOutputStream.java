package org.webseer.streams;

public interface BucketOutputStream<T> {

	public void writeObject(T object);

}
