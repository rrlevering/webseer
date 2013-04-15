package org.webseer.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;


public class GoogleWordFrequencyData {

	public static Iterator<WordFrequency> getWordFrequencies() throws IOException {
		InputStream input = GoogleWordFrequencyData.class.getResourceAsStream("/data/GoogleWordFrequency.txt");
		final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		return new Iterator<WordFrequency>() {

			String next = reader.readLine();

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public WordFrequency next() {
				String toReturn = next;
				try {
					next = reader.readLine();
				} catch (NumberFormatException e) {
					e.printStackTrace();
					next = null;
				} catch (IOException e) {
					e.printStackTrace();
					next = null;
				}
				String[] parts = toReturn.split(" ");
				return new WordFrequency(parts[0], Double.parseDouble(parts[1]));
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

}
