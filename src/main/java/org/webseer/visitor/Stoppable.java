package org.webseer.visitor;

/**
 * A stoppable class will be able to be stopped by a thread. Generally stoppable classes will check at appropriate
 * intervals to make sure their parent thread has not called stop() on the class. After stop() is called, the stoppable
 * object should not work until go() is once again called on the object.
 * 
 * @author Ryan Levering
 */
public interface Stoppable {

    /**
     * Stop this object from processing. This is not a hard thread stop, but implementing classes should wrap up what
     * they are doing currently. Most loop constructs should check once per loop to make sure the object hasn't been
     * stopped.
     */
    public void stop();

    /**
     * Tells the object it can go again. Generally this just resets the flag so the object can be restarted.
     */
    public void go();

    /**
     * Checks whether or not this object is supposed to be stopped. This can be checked via external classes, but is
     * also used internally for subclasses to check whether they have been stopped.
     * 
     * @return whether or not the object is stopped
     */
    public boolean isStopped();

}
