package org.webseer.transformation;

import org.webseer.model.meta.TransformationException;

/**
 * Transformers are at the core of webseer and define its power. Think of a transformer as a document transformation
 * function. It takes one or more objects at one or more input points and produces one or more objects at one or more
 * output points. The inputs are completely fuzzy and allow us to use the same transformer on multiple models that may
 * not share a similar structure. The outputs are not fuzzy and adhere to a model spec URI. The system is responsible
 * for assembling a path at runtime that defines how models flow through a series of transformations.
 * <p>
 * With pull transformations, as soon as the Iterators are set up on the input points and visitors are set up on the
 * output points, transform is called. The data in the iterators is not guaranteed to be all ready and in fact, most
 * often will not be. Calls to hasNext and next in the input iterators will block until data is ready or established to
 * be done.
 * 
 * @author Ryan Levering
 */
public interface PullRuntimeTransformation {

	/**
	 * Attempts to run the transformation. If the transformation can run, it will do so and fill the output channels. If
	 * it cannot, it will return false.
	 * 
	 * @throws RuntimeTransformationException if an execution error happens
	 * @throws TransformationException if something went wrong setting up the transformation
	 */
	public boolean transform() throws TransformationException, RuntimeTransformationException;

	/**
	 * Adds an output point that the function can write to.
	 */
	public void addOutputChannel(String outputPoint, ItemOutputStream<?> items);

	/**
	 * Adds an input point whereby the transformation can pull from when transform is run.
	 * 
	 * @param items an iterator which pulls from some other data bucket
	 * @throws TransformationException
	 */
	public void addInputChannel(String inputPoint, InputReader items) throws TransformationException;
	
	/**
	 * Adds a listener that gets notifications when the transformation is initializing, running, and done.
	 */
	public void addListener(TransformationListener listener);

}
