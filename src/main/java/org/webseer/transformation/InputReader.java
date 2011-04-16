package org.webseer.transformation;

import java.util.Iterator;

import org.webseer.model.runtime.InputQueue;
import org.webseer.model.trace.ItemView;

public interface InputReader {

	ItemInputStream getInputStream();

	Iterator<ItemView> getViews();

	InputQueue getQueue();

}
