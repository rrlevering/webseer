package org.webseer.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates a bunch of visitors to let models treat them as a single visitor. This allows multiple visitors to be
 * handled in the same model pass.
 * 
 * @author Ryan Levering
 */
public class AggregateSuperVisitor<T extends SuperVisitor> extends AbstractStoppableVisitor implements SuperVisitor {

    protected final List<T> visitors = new ArrayList<T>();

    /**
     * Adds a visitor to this aggregate visitor.
     * 
     * @param visitor the visitor to add to this aggregate so that any acceptor will accept the argument as well
     */
    public void addVisitor(T visitor) {
        this.visitors.add(visitor);
    }

    /**
     * Gets a list of the visitors in this aggregate.
     * 
     * @return an unmodifiable list of the visitors in this aggregate
     */
    public List<T> getVisitors() {
        return Collections.unmodifiableList(this.visitors);
    }

    /**
     * Enables this visitor to be able to run, which entails enabling all of the constituent visitors.
     */
    @Override
    public void go() {
        super.go();
        for (SuperVisitor visitor : this.visitors) {
            visitor.go();
        }
    }

    /**
     * Finalizes all of the constituent visitors.If the visitor is stopped, this done just returns.
     * 
     * @param acceptor the model that was just traversed
     */
    public void postTraverse(Object acceptor) {
        for (SuperVisitor visitor : this.visitors) {
            if (isStopped()) {
                return;
            }
            visitor.postTraverse(acceptor);
        }
    }

    /**
     * Delegates post-visit processing to all of the constituent visitors.If the visitor is stopped, this done just
     * returns.
     * 
     * @param acceptor the model element that was just visited
     */
    public void postVisit(Object acceptor) {
        for (SuperVisitor visitor : this.visitors) {
            if (isStopped()) {
                return;
            }
            visitor.postVisit(acceptor);
        }
    }

    /**
     * Initializes all of the constituent visitors.If the visitor is stopped, this done just returns.
     * 
     * @param acceptor the model that is about to be traversed
     */
    public void preTraverse(Object acceptor) {
        for (SuperVisitor visitor : this.visitors) {
            if (isStopped()) {
                return;
            }
            visitor.preTraverse(acceptor);
        }
    }

    /**
     * Delegates pre-visit processing to all of the constituent visitors.If the visitor is stopped, this done just
     * returns.
     * 
     * @param acceptor the model element that is about to be visited
     */
    public void preVisit(Object acceptor) {
        for (SuperVisitor visitor : this.visitors) {
            if (isStopped()) {
                return;
            }
            visitor.preVisit(acceptor);
        }
    }

    /**
     * Removes a visitor from this aggregate.
     * 
     * @param visitor the visitor to remove by equality
     * @return whether the visitor was found in this aggregate
     */
    public boolean removeVisitor(T visitor) {
        return this.visitors.remove(visitor);
    }

    /**
     * Stops this visitor, which entails stopping all of the constituent visitors as well.
     */
    @Override
    public void stop() {
        super.stop();
        for (SuperVisitor visitor : this.visitors) {
            visitor.stop();
        }
    }

    /**
     * Delegates the core model element processing to all of the constituent visitors. If the visitor is stopped, this
     * done just returns.
     * 
     * @param acceptor the model element that is being visited.
     */
    public void visit(Object acceptor) {
        for (SuperVisitor visitor : this.visitors) {
            if (isStopped()) {
                return;
            }
            visitor.visit(acceptor);
        }
    }

}
