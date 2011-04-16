package org.webseer.transformation;

import java.util.Iterator;

import org.webseer.model.trace.ItemView;

/**
 * A bucket reader creates streams from a particular bucket. These streams all pull on demand from the transformation
 * before and will pull from the underlying input queue.
 * 
 * @author ryan
 */
public interface EdgeReader {

	Iterator<ItemView> getViews();

}
