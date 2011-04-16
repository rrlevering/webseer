package org.webseer.visitor;

/**
 * A simple stoppable implementation that keeps the flag for implementing subclasses to check.
 * 
 * @author Ryan Levering
 */
public abstract class AbstractStoppableVisitor implements Stoppable {

    /**
     * The flag that is turned on and off, most often by the calling thread.
     */
    private boolean needsToStop = false;

    /**
     * Informs this object to stop. If the object has already been stopped or is waiting to be stopped, this method will
     * do nothing.
     */
    public void stop() {
        this.needsToStop = true;
    }

    /**
     * Informs this object to go. This is more semantically, "don't stop". This will not start the object, but will
     * enable it to be started.
     */
    public void go() {
        this.needsToStop = false;
    }

    /**
     * Checks whether the object has been stopped and therefore should not do any more work.
     * 
     * @return true if stop has been called on this object since either object creation or since go was last called
     */
    public boolean isStopped() {
        return this.needsToStop;
    }

}
