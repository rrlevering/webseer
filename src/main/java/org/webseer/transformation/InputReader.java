package org.webseer.transformation;

import java.util.Iterator;

import org.webseer.streams.model.runtime.InputQueue;
import org.webseer.streams.model.trace.ItemView;

public interface InputReader {

	ItemInputStream getInputStream();

	Iterator<ItemView> getViews();

	InputQueue getQueue();

}
