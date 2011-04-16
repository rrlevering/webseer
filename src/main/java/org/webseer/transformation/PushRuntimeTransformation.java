package org.webseer.transformation;

import org.webseer.visitor.SuperVisitor;

/**
 * Transformers are at the core of webseer and define its power. Think of a transformer as a document transformation
 * function. It takes one or more objects at one or more input points and produces one or more objects at one or more
 * output points. The inputs are completely fuzzy and allow us to use the same transformer on multiple models that may
 * not share a similar structure. The outputs are not fuzzy and adhere to a model spec URI. The system is responsible
 * for assembling a path at runtime that defines how models flow through a series of transformations.
 * 
 * @author Ryan Levering
 */
public interface PushRuntimeTransformation {

	/**
	 * Adds a listener to the transformation. This is what is used to detect generated models.
	 * 
	 * @param listener the listener that gets notified of new outputs
	 */
	public void addOutputChannel(String outputPoint, SuperVisitor listener);

	/**
	 * Gets the visitor for a particular input point of the transformation.
	 * 
	 * @param inputPoint the input point to get the visitor for
	 * @return the visitor that is responsible for handling a certain type of input
	 */
	public SuperVisitor getInputChannel(String inputPoint);

}
