package org.webseer.classification;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.webseer.transformation.InputChannel;
import org.webseer.transformation.OutputChannel;
import org.webseer.transformation.java.JavaFunction;

public class FeaturizeTokens implements JavaFunction {

	@InputChannel
	String toFeaturize;

	@OutputChannel
	FeatureRecord record;

	@Override
	public void execute() throws Throwable {
		Map<String, Integer> counts = new HashMap<String, Integer>();
		StringTokenizer tokenizer = new StringTokenizer(toFeaturize);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (counts.containsKey(token)) {
				counts.put(token, counts.get(token) + 1);
			} else {
				counts.put(token, 1);
			}
		}
		record = new FeatureRecord();
		record.features = new String[counts.size()];
		record.values = new int[counts.size()];
		int pos = 0;
		for (Entry<String, Integer> entry : counts.entrySet()) {
			record.features[pos] = entry.getKey();
			record.values[pos] = entry.getValue();
			pos++;
		}
	}

}
