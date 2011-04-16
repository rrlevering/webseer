package org.webseer.transformation;

/**
 * This is an interface that is used to work with simple java functional objects.
 * 
 * @author ryan
 */
public interface JavaFunction {

	/**
	 * Initialize is called once per function before the first time it is executed. This allows you to deal with inputs
	 * that are expected to be dealt with only a single time.
	 */
	// public void initialize() throws Throwable;

	/**
	 * Execute is called every time a set of inputs is ready in the inputs of a function.
	 */
	public void execute() throws Throwable;

}
