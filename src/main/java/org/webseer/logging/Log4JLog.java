package org.webseer.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author Ryan Levering
 */
public class Log4JLog extends Log {
    
    public void logDebug(String category, String message) {
        Logger.getLogger(category).debug(message);
    }

    public void logInfo(String category, String message) {
        Logger.getLogger(category).info(message);
    }

    public void logError(String category, String message) {
        Logger.getLogger(category).error(message);
    }

    public void logException(String category, String message) {
        Logger.getLogger(category).warn(message);
    }

    public void logTest(String category, String message) {
        Logger logger = Logger.getLogger("TEST " + category);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logger.info(message);
        logger.setLevel(oldLevel);
    }

    public LocalLog getLocalLog(String category) {
        return new LocalLog4JLog(category);
    }
    
    private class LocalLog4JLog implements LocalLog {
        
        private String category;
        
        private LocalLog4JLog(String category) {
            this.category = category;
        }
        
        public void logDebug(String message, Object... variables) {
            for (int i = 0; i < variables.length; i++) {
                message = message.replace("%" + (i + 1), String.valueOf(variables[i]));
            }
            Log4JLog.this.logDebug(this.category, message);
        }

        public void logInfo(String message, Object... variables) {
            for (int i = 0; i < variables.length; i++) {
                message = message.replace("%" + (i + 1), String.valueOf(variables[i]));
            }
            Log4JLog.this.logInfo(this.category, message);
        }

        public void logError(String message, Object... variables) {
            for (int i = 0; i < variables.length; i++) {
                message = message.replace("%" + (i + 1), String.valueOf(variables[i]));
            }
            Log4JLog.this.logError(this.category, message);
        }

        public void logException(String message, Object... variables) {
            for (int i = 0; i < variables.length; i++) {
                message = message.replace("%" + (i + 1), String.valueOf(variables[i]));
            }
            Log4JLog.this.logException(this.category, message);
        }

        public void logTest(String message, Object... variables) {
            for (int i = 0; i < variables.length; i++) {
                message = message.replace("%" + (i + 1), String.valueOf(variables[i]));
            }
            Log4JLog.this.logTest(this.category, message);
        }
        
    }

}