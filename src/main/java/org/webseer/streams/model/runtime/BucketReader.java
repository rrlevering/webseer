package org.webseer.streams.model.runtime;

import java.util.Iterator;

import org.webseer.streams.model.trace.Item;

public interface BucketReader {

	Iterator<Item> getItems();

}
