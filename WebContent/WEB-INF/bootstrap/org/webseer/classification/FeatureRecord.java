package org.webseer.classification;

import org.webseer.type.Type;

@Type
public class FeatureRecord {

	public String[] features;

	public int[] values;

	public String toString() {
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < features.length; i++) {
			output.append(features[i] + "=" + values[i] + " ");
		}
		return output.toString();
	}

}
