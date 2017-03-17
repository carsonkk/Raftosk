package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.Command;
import main.java.com.carsonkk.raftosk.global.SysLog;

import java.io.Serializable;
import java.util.UUID;

// Keep track of info for each entry into the system's transaction log
public class LogEntry implements Serializable {
    //region Private Members

    private int index;
    private int term;
    private Command command;

    //endregion

    //region Constructors

    public LogEntry() {
        this.index = -1;
        this.term = -1;
        this.command = new Command();
        SysLog.logger.finer("Created new log entry");
    }

    public LogEntry(int index) {
        this();
        this.index = index;
        SysLog.logger.finer("Created new log entry with index " + this.index);
    }

    public LogEntry(int index, int term, Command command) {
        this(index);
        this.term = term;
        this.command = command;
        SysLog.logger.finer("Created new log entry with term " + this.term + " and command " +
                this.command.getCommandType());
    }

    public int getIndex() {
        return this.index;
    }

    public int getTerm() {
        return this.term;
    }

    public Command getCommand() {
        return this.command;
    }

    //endregion
}
