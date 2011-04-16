package org.webseer.transformation;

import java.util.Iterator;

import org.webseer.model.trace.Item;

public interface BucketReader {

	Iterator<Item> getItems();

}
