package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.Command;

public class LogEntry {
    private int index;
    private int term;
    private int serverId;
    private Command command;

    public LogEntry() {
        this.index = -1;
        this.term = -1;
        this.serverId = -1;
        this.command = new Command();
    }

    public LogEntry(int index) {
        this();
        this.index = index;
    }

    public LogEntry(int index, int term, int serverId) {
        this(index);
        this.term = term;
        this.serverId = serverId;
    }
}
