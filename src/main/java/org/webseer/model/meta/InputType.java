package org.webseer.model.meta;

/**
 * By defining different input types to transformations, you 1) affect the inputs to the transformations and 2) affect
 * their castability. If the transformation needs the entire bucket to make a decision (think sort or sum or some other
 * group operation), than choose aggregate type. If the processing is going to be done on each item in a bucket
 * separately (counting the number of apostrophes in a String), choose serial. If the transformation is some sort of
 * combinatorial transformation (word counts of a static word list on a set of documents), choose distribute.
 * <p>
 * Note the aggregate behavior of a transformation is really defined by the combination of these input types on all of
 * the inputs. For instance, if the inputs are:
 * 
 * <pre>
 * Case 1
 * A (DISTRIBUTE): PQR
 * B (SERIAL): XYZ
 * => PX, QX, RX, PY, QY, RY, PZ, QZ, RZ
 * </pre>
 * 
 * Reversing the input types will result in the same combinatorial in a different order:
 * 
 * <pre>
 * Case 2
 * A (SERIAL): PQR
 * B (DISTRIBUTE): XYZ
 * => PX, PY, PZ, QX, QY, QZ, RX, RY, RZ
 * </pre>
 * 
 * Multiple distributes will result in an undefined ordering if there are only distributes in the inputs. Multiple
 * serials will be taken sequentially:
 * 
 * <pre>
 * Case 3
 * A (SERIAL): PQR
 * B (DISTRIBUTE): XYZ
 * => PX, QY, RZ
 * </pre>
 * 
 * Mixed with distributes will have predictable effect:
 * 
 * <pre>
 * Case 4
 * A (DISTRIBUTE): PQR
 * B (SERIAL): XYZ
 * C (SERIAL): LMN
 * => PXL, QXL, RXL, PYM, QYM, RYM, PZN, QZN, RZN
 * </pre>
 * 
 * Finally, aggregate means apply the whole bucket to the transformation (on the appropriate channel):
 * 
 * <pre>
 * A (AGGREGATE): PQR
 * B (SERIAL): XYZ
 * => PQRX, PQRY, PQRZ
 * </pre>
 * 
 * Note that aggregate functions by their definition lose serious castability (how do we know exactly which one in that
 * bucket resulted in a certain output), plus they can't be as parallelized, so it's best to use non-aggregate input
 * types when you can.
 * <p>
 * If you know map reduce, it could be represented as two transformations: the first a single interleave input
 * transformation and the second a single aggregate input transformation.
 */
public enum InputType {
	/**
	 * Aggregate means that the entire bucket is applied to every other combination of inputs from the other buckets.
	 */
	AGGREGATE,
	/**
	 * Interleave means that every input in a bucket will be used in order, one at a time to the other combinations of
	 * inputs.
	 */
	SERIAL,
	/**
	 * Pull the value from configuration parameters on the node. These are saved with the runtime graph.
	 */
	CONFIGURATION
}
