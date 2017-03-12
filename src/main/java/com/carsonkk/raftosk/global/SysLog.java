package main.java.com.carsonkk.raftosk.global;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// Global project handler for all logging
public final class SysLog {
    //region Public Members

    public static Logger logger;

    //endregion

    //region Public Methods

    // Initialize the logger, handlers, and formatter
    public static void setup(int level) {
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());

        switch (level) {
            case 0: {
                logger.setLevel(Level.OFF);
                break;
            }
            case 1: {
                logger.setLevel(Level.SEVERE);
                break;
            }
            case 2: {
                logger.setLevel(Level.WARNING);
                break;
            }
            case 3: {
                logger.setLevel(Level.INFO);
                break;
            }
            case 4: {
                logger.setLevel(Level.CONFIG);
                break;
            }
            case 5: {
                logger.setLevel(Level.FINE);
                break;
            }
            case 6: {
                logger.setLevel(Level.FINER);
                break;
            }
            case 7: {
                logger.setLevel(Level.FINEST);
                break;
            }
            case 8: {
                logger.setLevel(Level.ALL);
                break;
            }
        }

        handler.setLevel(logger.getLevel());
        logger.addHandler(handler);
    }

    //endregion
}
