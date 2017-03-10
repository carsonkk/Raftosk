package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.Command;
import main.java.com.carsonkk.raftosk.global.CommandType;
import main.java.com.carsonkk.raftosk.global.RPCInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

// Manage proper data replication within server log
public class ConsensusModule implements Runnable {
    private Thread thread;
    private String threadName;
    private int serverId;

    public ConsensusModule() {
        super();
        threadName = "ConsensusModuleThread";
        thread = new Thread(this, threadName);
    }

    public ConsensusModule(int serverId) {
        this();
        this.serverId = serverId;
    }

    public Thread getThread() {
        return this.thread;
    }

    @Override
    public void run() {

    }
}
