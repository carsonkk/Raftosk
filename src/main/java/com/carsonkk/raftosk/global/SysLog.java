package main.java.com.carsonkk.raftosk.global;

import java.util.logging.*;

// Global project handler for all logging
public final class SysLog {
    //region Public Members

    public static Logger logger;
    public static int level;

    //endregion

    //region Public Methods

    // Initialize the logger, handlers, and formatter
    public static void setup(int logLevel, RaftoskType raftoskType) {
        String baseFileHandler = "%h/Code/-Repos/Raftosk/out/logs/";
        level = logLevel;
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

        switch (logLevel) {
            case 0: {
                // Turns off all logging to the console/log files
                logger.setLevel(Level.OFF);
                break;
            }
            case 1: {
                // SEVERE: Errors or system-halting exceptions
                logger.setLevel(Level.SEVERE);
                break;
            }
            case 2: {
                // WARNING: Potentially unexpected behavior or minor exceptions
                logger.setLevel(Level.WARNING);
                break;
            }
            case 3: {
                // INFO: General execution information, default level for "easily-read" logs
                logger.setLevel(Level.INFO);
                break;
            }
            case 4: {
                // CONFIG: Extra configuration details for objects created at most a couple times
                // Anything beyond this level is extremely verbose
                logger.setLevel(Level.CONFIG);
                break;
            }
            case 5: {
                // FINE: State machine state updates and send/receive heartbeats
                logger.setLevel(Level.FINE);
                break;
            }
            case 6: {
                // FINER: Object creation
                logger.setLevel(Level.FINER);
                break;
            }
            case 7: {
                // FINEST: Method entry/exit
                logger.setLevel(Level.FINEST);
                break;
            }
            case 8: {
                // ALL: Everything listed above (equivalent to FINEST)
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
