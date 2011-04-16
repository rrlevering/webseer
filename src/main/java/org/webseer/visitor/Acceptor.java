package org.webseer.visitor;

/**
 * This interface serves as the highest level interface for the composite pattern, accepting visitors and allowing
 * infinitely deep aggregation of Acceptors.
 * 
 * @author Ryan Levering
 */
public interface Acceptor {

    /**
     * Subclasses should call visit, passing themselves as the argument. They are also responsible for passing on the
     * accept call to their children/edges/etc. depending on what the model type is. The order in which the model does
     * this determines the underlying visitor traversal strategy.
     * 
     * @param visitor the visitor to accept
     */
    public void accept(SuperVisitor visitor);

}
