package main.java.com.carsonkk.raftosk.global;

import main.java.com.carsonkk.raftosk.server.LogEntry;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.List;

// Object returned by any RPC call, contains integer and boolean values for result handling
public class ReturnValueRPC implements Serializable {
    //region Private Members

    // Primary return data
    private int serverId;
    private int value;
    private boolean condition;

    //Secondary return data (may not always be set)
    private int ticketPool;
    private List<LogEntry> log;
    private List<Integer> nextIndexes;
    private int nextIndex;
    private int commitIndex;

    //endregion

    //region Constructors

    public ReturnValueRPC() throws RemoteException {
        this.serverId = 0;
        this.value = 0;
        this.condition = false;
        this.ticketPool = -1;
        this.log = null;
        this.nextIndexes = null;
        this.nextIndex = -1;
        this.commitIndex = -1;

        SysLog.logger.finer("Created a new return value RPC");
    }

    //endregion

    //region Getters/Setters

    public int getServerId() {
        return this.serverId;
    }

    public void setServerId(int serverId) {
        this.serverId = serverId;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean getCondition() {
        return this.condition;
    }

    public void setCondition(boolean condition) {
        this.condition = condition;
    }

    public int getTicketPool() {
        return this.ticketPool;
    }

    public void setTicketPool(int ticketPool) {
        this.ticketPool = ticketPool;
    }

    public List<LogEntry> getLog() {
        return this.log;
    }

    public void setLog(List<LogEntry> log) {
        this.log = log;
    }

    public List<Integer> getNextIndexes() {
        return this.nextIndexes;
    }

    public int getNextIndex() {
        return this.nextIndex;
    }

    public void setNextIndex(int nextIndex) {
        this.nextIndex = nextIndex;
    }

    public int getCommitIndex() {
        return this.commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    //endregion
}
