package main.java.com.carsonkk.raftosk.global;

import java.util.logging.*;

// Global project handler for all logging
public final class SysLog {
    //region Public Members

    public static Logger logger;

    //endregion

    //region Public Methods

    // Initialize the logger, handlers, and formatter
    public static void setup(int level, RaftoskType raftoskType) {
        String baseFileHandler = "%h/Code/-Repos/Raftosk/out/logs/";
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        FileHandler fileHandler;

        try {
            switch (raftoskType) {
                case SERVER: {
                    fileHandler = new FileHandler(baseFileHandler + "server-%u.log");
                    break;
                }
                case ADMINISTRATOR: {
                    fileHandler = new FileHandler(baseFileHandler + "administrator-%u.log");
                    break;
                }
                case CUSTOMER: {
                    fileHandler = new FileHandler(baseFileHandler + "customer-%u.log");
                    break;
                }
                default: {
                    System.out.println("Received an invalid raftosk type while setting up SysLog: " + raftoskType);
                    return;
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }

        consoleHandler.setFormatter(new SysLogFormatter());
        fileHandler.setFormatter(new SysLogFormatter());

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

        consoleHandler.setLevel(logger.getLevel());
        fileHandler.setLevel(logger.getLevel());
        logger.addHandler(consoleHandler);
        logger.addHandler(fileHandler);
    }

    //endregion
}
