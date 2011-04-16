package org.webseer.logging;

public interface LocalLog {

    /**
     * Info level is for messages that are useful for understanding what's going on in the system, but not so low level
     * that a common user wouldn't understand.
     * 
     * @param category the category of the information
     * @param message the message describing the information
     */
    public abstract void logInfo(String message, Object... variables);

    /**
     * An error is something that is wrong with the current application or not finished so not handling correctly.
     * 
     * @param category the category of the error
     * @param message the message describing the error
     */
    public abstract void logError(String message, Object... variables);

    /**
     * An exception is any error that occurs with the page content that is due to poor content as opposed to a program
     * error. This would most likely not display correctly in a browser or throw an error.
     * 
     * @param category the category of the exception
     * @param message the message describing the exception
     */
    public abstract void logException(String message, Object... variables);

    /**
     * This is used for debugging information that is really only useful before release. Hopefully most people won't see
     * a lot of these.
     * 
     * @param category the category of the debug
     * @param message the message describing the debug
     */
    public abstract void logDebug(String message, Object... variables);

    /**
     * This is the level that you should log tests at.  It should probably output to something similar to an info level.
     * 
     * @param message the message describing the test information
     * @param variables variables to fill in the message
     */
    public abstract void logTest(String message, Object... variables);
    
}
