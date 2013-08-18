package org.webseer.bucket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.webseer.transformation.InputReader;
import org.webseer.transformation.OutputWriter;

public class TransientMemoryBucket implements LogBucket {

	private final List<Data> items = new ArrayList<Data>();
	
	@Override
	public InputReader getBucketReader() {
		return new TransientMemoryReader();
	}

	@Override
	public OutputWriter getBucketWriter() {
		return new TransientMemoryWriter();
	}
	
	public class TransientMemoryReader implements InputReader {

		@Override
		public Iterator<Data> iterator() {
			return items.iterator();
		}
		
	}
	
	public class TransientMemoryWriter implements OutputWriter {

		@Override
		public void writeData(Data item) {
			items.add(item);
		}
		
	}

}
