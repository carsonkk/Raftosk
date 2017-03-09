package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.Command;

public class LogEntry {
    private int term;
    private int serverId;
    private Command command;

    public LogEntry() {
        term = -1;
        serverId = -1;
        command = new Command();
    }
}
