package org.webseer.logging;

/**
 * This will serve as an interface and factory for any logging that the application does.
 * 
 * @author Ryan Levering
 */
public abstract class Log {

    private static Log log;

    public static Log getInstance() {
        if (log == null) {
            log = new Log4JLog();
        }
        return log;
    }
    
    public static LocalLog getInstance(String category) {
        return getInstance().getLocalLog(category);
    }

    public static void setLog(Log log) {
        Log.log = log;
    }

    /**
     * An exception is any error that occurs with the page content that is due to poor content as opposed to a program
     * error. This would most likely not display correctly in a browser or throw an error.
     * 
     * @param category the category of the information
     * @param message the message describing the information
     */
    public abstract void logInfo(String category, String message);

    /**
     * An error is something that is wrong with the current application or not finished so not handling correctly.
     * 
     * @param category the category of the error
     * @param message the message describing the error
     */
    public abstract void logError(String category, String message);

    /**
     * An exception is any error that occurs with the page content that is due to poor content as opposed to a program
     * error. This would most likely not display correctly in a browser or throw an error.
     * 
     * @param category the category of the exception
     * @param message the message describing the exception
     */
    public abstract void logException(String category, String message);

    /**
     * This is used for debugging information that is really only useful before release. Hopefully most people won't see
     * a lot of these.
     * 
     * @param category the category of the debug
     * @param message the message describing the debug
     */
    public abstract void logDebug(String category, String message);
    
    /**
     * This is used for test messages that should only appear when tests are being run, not during normal system usage.
     * 
     * @param category the category of the debug
     * @param message the message describing the debug
     */
    public abstract void logTest(String category, String message);
    
    public abstract LocalLog getLocalLog(String category);
}
