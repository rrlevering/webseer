package org.tartarus.snowball;

import java.util.ArrayList;
import java.util.List;

import org.tartarus.snowball.ext.englishStemmer;
import org.webseer.java.FunctionDef;

public class Stemmer {

	/**
	 * Stems words using a snowball stemmer: http://snowball.tartarus.org/.
	 * 
	 * @param words
	 *            the words to stem
	 * @return a stemmed list of words in the same order
	 */
	@FunctionDef
	public Iterable<String> stem(Iterable<String> words) {
		englishStemmer stemmer = new englishStemmer();

		List<String> stemmed = new ArrayList<String>();
		for (String word : words) {
			stemmer.setCurrent(word);
			stemmer.stem();
			stemmed.add(stemmer.getCurrent());
		}
		return stemmed;
	}

}
