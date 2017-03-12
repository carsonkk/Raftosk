package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.Command;
import main.java.com.carsonkk.raftosk.global.SysLog;

// Keep track of info for each entry into the system's transaction log
public class LogEntry {
    //region Private Members

    private int index;
    private int term;
    private int serverId;
    private Command command;

    //endregion

    //region Constructors

    public LogEntry() {
        this.index = -1;
        this.term = -1;
        this.serverId = -1;
        this.command = new Command();
        SysLog.logger.finer("Created new log entry");
    }

    public LogEntry(int index) {
        this();
        this.index = index;
        SysLog.logger.finer("Created new log entry with index " + this.index);
    }

    public LogEntry(int index, int term, int serverId) {
        this(index);
        this.term = term;
        this.serverId = serverId;
        SysLog.logger.finer("Created new log entry with term " + this.term + " and server ID " + this.serverId);
    }

    //endregion
}
