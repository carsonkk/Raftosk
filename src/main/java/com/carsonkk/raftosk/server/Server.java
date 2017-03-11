package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.RPCInterface;
import main.java.com.carsonkk.raftosk.global.ServerProperties;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Top-level server functionality
public class Server {
    //region Private members

    private int serverId;
    private StateMachine stateMachine;
    private Future<Void> stateMachineFuture;
    private HandleRPC serverBinding;

    //endregion

    //region Constructors

    public Server(int serverId) {
        this.serverId = serverId;
        this.stateMachine = new StateMachine(this);
        this.stateMachineFuture = null;
        try {
            serverBinding = new HandleRPC(this);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //endregion

    //region Getters/Setters

    public int getServerId() {
        return this.serverId;
    }

    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    //endregion

    //region Public Methods

    public void initializeServer() {
        // Register server with RMI, use base port + id as port value
        try {
            if(!this.serverBinding.setupConnection()) {
                return;
            }
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

        // Setup state to be a follower
        synchronized (this.stateMachine.getCurrentState()) {
            this.stateMachine.setCurrentState(StateType.FOLLOWER);
        }

        // Start up state machine
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        this.stateMachineFuture = executorService.submit(this.stateMachine);
    }

    //endregion
}