package org.webseer.streams.model.runtime;

import java.io.IOException;
import java.io.OutputStream;

import org.webseer.bucket.Data;
import org.webseer.streams.model.runtime.RuntimeConfigurationImpl.OutputGroupGetter;
import org.webseer.streams.model.trace.DataItem;
import org.webseer.transformation.OutputWriter;

public class ItemOutputStream extends OutputStream implements OutputWriter {

	private final OutputGroupGetter groupGetter;

	private OutputStream currentStream;

	private DataItem currentItem;

	public ItemOutputStream(OutputGroupGetter outputGroupGetter) {
		this.groupGetter = outputGroupGetter;
	}

	private OutputStream createStream() {
		if (currentStream == null) {
			currentStream = currentItem.getOutputStream();
		}
		return currentStream;
	}

	@Override
	public void close() throws IOException {
		createStream().close();
	}

	@Override
	public void flush() throws IOException {
		createStream().flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		createStream().write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		createStream().write(b);
	}

	@Override
	public void write(int b) throws IOException {
		createStream().write(b);
	}

	public void startNewItem() {
		try {
			if (currentStream != null) {
				currentStream.close();
				currentStream = null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		currentItem = new DataItem(this.groupGetter.getNeoService(), this.groupGetter.getOutputGroup());
	}

	public void writeData(Data data) {
		startNewItem();
		currentItem.getType().setValue(currentItem, data);
	}

}
