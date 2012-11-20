package org.webseer.streams.model.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webseer.model.meta.UserType;
import org.webseer.streams.model.runtime.RuntimeConfigurationImpl.InputGroupGetter;
import org.webseer.streams.model.trace.InputGroup;
import org.webseer.streams.model.trace.ItemView;
import org.webseer.type.TypeFactory;

/**
 * This is an input stream that can be used to read values from transformation edges. This is done differently depending
 * on the type of data. For primitive values, the values are gleaned from the properties set on the DB items. For byte
 * streams or complex objects they can be read directly from a persistent store.
 * <p>
 * The stream concept is very similar to the way Java does ZipInputStreams. The read methods will only read up to the
 * end of the current entry in the stream unless you advance it with nextItem.
 * 
 * @author ryan
 */
public class ItemInputStream extends InputStream implements Iterator<Object> {

	private static final Logger log = LoggerFactory.getLogger(ItemInputStream.class);

	private final Iterator<ItemView> underlyingItems;

	private final UserType targetType;

	private final InputGroupGetter getter;

	private ItemView currentItem;

	private InputStream currentStream;

	public ItemInputStream(Iterator<ItemView> items, UserType targetType, InputGroupGetter getter) {
		this.underlyingItems = items;
		this.targetType = targetType;
		this.getter = getter;
	}

	public ItemInputStream() {
		this.underlyingItems = Collections.<ItemView>emptyList().iterator();
		this.targetType = null;
		this.getter = null;
	}

	public ItemInputStream(Iterator<ItemView> items) {
		this.underlyingItems = items;
		this.targetType = null;
		this.getter = null;
	}

	private InputStream createStream() {
		if (currentStream == null) {
			currentStream = currentItem.getViewData().getInputStream();
		}
		return currentStream;
	}

	public int read() throws IOException {
		return createStream().read();
	}

	@Override
	public int available() throws IOException {
		return createStream().available();
	}

	@Override
	public void close() throws IOException {
		createStream().close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		createStream().mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return createStream().markSupported();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return createStream().read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return createStream().read(b);
	}

	@Override
	public synchronized void reset() throws IOException {
		createStream().reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return createStream().skip(n);
	}
	
	public ItemView getCurrentView() {
		if (currentItem == null) {
			throw new IllegalStateException();
		}
		
		return currentItem;
	}

	public Object getCurrent() {
		ItemView currentItem = getCurrentView();

		if (targetType == null) {
			return currentItem.get();
		}

		Object original;
		UserType type;
		original = currentItem.get();
		type = currentItem.getType();

		return TypeFactory.getTypeFactory(getter.getNeoService()).cast(original, type, targetType);
	}

	public boolean hasNext() {
		return underlyingItems.hasNext();
	}

	public Object next() {
		nextView();
		return getCurrent();
	}
	
	public ItemView nextView() {
		if (!underlyingItems.hasNext()) {
			throw new IllegalStateException();
		}

		InputGroup inputGroup = getter.getInputGroup();

		currentItem = underlyingItems.next();
		if (inputGroup != null) {
			log.info("Advancing input group");
			inputGroup.advance();
		}
		try {
			if (currentStream != null) {
				currentStream.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return currentItem;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
