package org.webseer.visitor;

/**
 * A super visitor is a general extension of the visitor pattern. It has additional methods for doing
 * initialization/finalization of the visitor (pre/postTraverse) and for pre/post processing of a particular node
 * (pre/postVisit). This is useful for handling post/pre children traversal, for instance.
 * <p>
 * This interface also adds the stoppable interface, which means that any visitor implementations should check at the
 * beginning of their visit methods to make sure the object hasn't been stopped.
 * <p>
 * Finally, this visitor implementation takes the approach of not forcing dynamic dispatch. While dynamic dispatch in
 * the visitor pattern does make it more magical, it also makes visitors harder to write. The magic in this library is
 * in the reflection-based implementation of this interface which essentially does runtime double dispatch.
 * 
 * @author Ryan Levering
 */
public interface SuperVisitor extends Stoppable {

	/**
	 * Finalizes the visitor, it is expected that no "visit" type method will be called between this method and the next
	 * preTraverse call.
	 * 
	 * @param acceptor generally the outermost model object that is being traversed
	 */
	public void postTraverse(Object acceptor) throws VisitorException;

	/**
	 * Does any post-visit handling of a model element.
	 * 
	 * @param acceptor the generic object that was just visited
	 */
	public void postVisit(Object acceptor) throws VisitorException;

	/**
	 * Initializes the visitor before it traverses a model structure.
	 * 
	 * @param acceptor generally the outermost model object that is being traversed
	 */
	public void preTraverse(Object acceptor) throws VisitorException;

	/**
	 * Does any pre-visit handling of a model element.
	 * 
	 * @param acceptor the generic object that is about to be visited
	 */
	public void preVisit(Object acceptor) throws VisitorException;

	/**
	 * Does the core handling of the model element.
	 * 
	 * @param acceptor the generic object that is being visited
	 */
	public void visit(Object acceptor) throws VisitorException;

}
