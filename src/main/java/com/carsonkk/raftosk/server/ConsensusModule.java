package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.SysLog;

import java.util.concurrent.Callable;

// Manage proper data replication within server transaction log
public class ConsensusModule implements Callable<Void> {
    //region Private Members

    private int serverId;

    //endregion

    //region Constructors

    public ConsensusModule() {
        SysLog.logger.finer("Created new consensus module");
    }

    public ConsensusModule(int serverId) {
        this();
        this.serverId = serverId;
        SysLog.logger.finer("Created new consensus module with server ID " + this.serverId);
    }

    //endregion

    //region Public Methods

    @Override
    public Void call() {
        return null;
    }

    //endregion
}
