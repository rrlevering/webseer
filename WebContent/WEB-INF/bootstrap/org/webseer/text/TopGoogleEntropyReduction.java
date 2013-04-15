package org.webseer.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.webseer.java.FunctionDef;

/**
 * Reduces concatenated phrases to words by entropy reduction based on word
 * frequencies from the Google corpus, compiled by the Department of Psychology
 * at Ohio State University and found at
 * http://mall.psy.ohio-state.edu/wiki/images/0/07/GoogleWordFrequency.txt;
 * 
 * @author Ryan Levering (rrlevering@gmail.com)
 */
public class TopGoogleEntropyReduction {

	Map<String, Double> probabilities = new HashMap<String, Double>();

	public TopGoogleEntropyReduction() throws NumberFormatException, IOException {
		Iterator<WordFrequency> frequencies = GoogleWordFrequencyData.getWordFrequencies();
		while (frequencies.hasNext()) {
			WordFrequency frequency = frequencies.next();
			probabilities.put(frequency.getWord(), frequency.getFrequency() / 1000000);
			
		}
	}

	@FunctionDef
	public Iterable<String> reduce(Iterable<String> words) {
		List<String> reduced = new ArrayList<String>();
		for (String word : words) {
			reduced.addAll(recursiveSegment(word));
		}
		return reduced;
	}

	private List<String> recursiveSegment(String word) {
		double bestProb = getProbability(word);

		int best = -1;
		for (int i = 1; i < word.length() - 1; i++) {
			String first = word.substring(0, i);
			String second = word.substring(i);

			double firstProb = getProbability(first);
			double secondProb = getProbability(second);

			if (firstProb * secondProb > bestProb) {
				best = i;
				bestProb = firstProb * secondProb;
			}
		}

		if (best < 0) {
			return Collections.singletonList(word);
		}
		
		String first = word.substring(0, best);
		String second = word.substring(best);
		
		List<String> merged = new ArrayList<String>();
		merged.addAll(recursiveSegment(first));
		merged.addAll(recursiveSegment(second));
		return merged;
	}

	private final double getProbability(String word) {
		return probabilities.containsKey(word) ? probabilities.get(word) : 0;
	}

}
