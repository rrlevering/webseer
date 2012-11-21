package org.webseer.classification;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.webseer.java.FunctionDef;
import org.webseer.java.InputChannel;
import org.webseer.java.JavaFunction;
import org.webseer.java.OutputChannel;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SparseInstance;

import com.google.protobuf.ByteString;

@FunctionDef
public class BinarySVMClassifier implements JavaFunction {

	@InputChannel
	private Iterable<FeatureRecord> firstClass;

	@InputChannel
	private Iterable<FeatureRecord> secondClass;

	@OutputChannel
	private ByteString classifier;

	@Override
	public void execute() throws Throwable {
		Instances wekaSet = convertToWeka(firstClass, secondClass);
		weka.classifiers.Classifier wekaClassifier = getWekaClassifier();
		try {
			wekaClassifier.buildClassifier(wekaSet);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(stream);
		objectStream.writeObject(wekaClassifier);
		classifier = ByteString.copyFrom(stream.toByteArray());
	}

	protected Classifier getWekaClassifier() {
		SMO smo = new SMO();
		smo.setBuildLogisticModels(true);
		return smo;
	}

	static Instances convertToWeka(Iterable<FeatureRecord> firstClass, Iterable<FeatureRecord> secondClass) {
		FastVector attributes = new FastVector();

		Map<String, Attribute> cachedAttributes = new HashMap<String, Attribute>();

		FastVector featureValues = new FastVector();
		featureValues.addElement("Class1");
		featureValues.addElement("Class2");

		Attribute classAttribute = new Attribute("WekaClassName", featureValues);
		attributes.addElement(classAttribute);

		for (FeatureRecord record : firstClass) {
			for (int i = 0; i < record.features.length; i++) {
				String feature = record.features[i];

				Attribute attr = cachedAttributes.get(feature);
				if (attr == null) {
					attr = new Attribute(feature);
					cachedAttributes.put(feature, attr);
					attributes.addElement(attr);
				}
			}
		}
		for (FeatureRecord record : secondClass) {
			for (int i = 0; i < record.features.length; i++) {
				String feature = record.features[i];

				Attribute attr = cachedAttributes.get(feature);
				if (attr == null) {
					attr = new Attribute(feature);
					cachedAttributes.put(feature, attr);
					attributes.addElement(attr);
				}
			}
		}

		System.out.println("Number of attributes = " + attributes.size());

		Instances instanceObject = new Instances("Imported data", attributes, 0);
		instanceObject.setClass(classAttribute);
		for (FeatureRecord record : firstClass) {
			System.out.println("Adding first class");
			SparseInstance wekaRecord = new SparseInstance(attributes.size());

			wekaRecord.setValue(classAttribute, "Class1");

			for (int i = 0; i < record.features.length; i++) {
				String feature = record.features[i];
				int featureValue = record.values[i];

				Attribute attr = cachedAttributes.get(feature);

				wekaRecord.setValue(attr, featureValue);
			}
			instanceObject.add(wekaRecord);
		}
		for (FeatureRecord record : secondClass) {
			System.out.println("Adding second class");
			SparseInstance wekaRecord = new SparseInstance(attributes.size());

			wekaRecord.setValue(classAttribute, "Class2");

			for (int i = 0; i < record.features.length; i++) {
				String feature = record.features[i];
				int featureValue = record.values[i];

				Attribute attr = cachedAttributes.get(feature);

				wekaRecord.setValue(attr, featureValue);
			}
			instanceObject.add(wekaRecord);
		}
		return instanceObject;
	}
}
