package org.webseer.transformation;

import java.util.Iterator;

import org.webseer.streams.model.trace.Item;

public interface BucketReader {

	Iterator<Item> getItems();

}
