package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.RPCInterface;
import main.java.com.carsonkk.raftosk.global.ServerProperties;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Server {
    private int serverId;
    private StateMachine stateMachine;
    private ConsensusModule consensusModule;

    public Server() throws RemoteException {
        this.serverId = -1;
        this.stateMachine = new StateMachine();
        this.consensusModule = new ConsensusModule();
    }

    public Server(int serverId) throws RemoteException {
        this();
        this.serverId = serverId;
    }

    public int initializeServer() throws RemoteException {
        // Register server with RMI, use base port + id as port value
        if(this.consensusModule.setupConnection(ServerProperties.getBaseServerPort() + this.serverId) == 1) {
            return 1;
        }
        // Update state to be a follower
        synchronized (this.stateMachine.getCurrentState()) {
            this.stateMachine.setCurrentState(StateType.FOLLOWER);
        }

        return 0;
    }

}