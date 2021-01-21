package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerManager {
    private static Logger logger;

    private LoggerManager() {
    }

    public static synchronized Logger getInstance() {
        if (logger != null) {
            logger = LogManager.getLogger();
        }
        return logger;
    }
}
