package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.ServerProperties;
import main.java.com.carsonkk.raftosk.global.SysLog;

import java.rmi.RemoteException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Top-level server functionality
public class Server {
    //region Private members

    private final ReentrantLock ticketPoolLock = new ReentrantLock(true);

    private int serverId;
    private StateMachine stateMachine;
    private HandleRPC serverBinding;
    private int ticketPool;

    //endregion

    //region Constructors

    public Server(int serverId) {
        this.serverId = serverId;
        this.stateMachine = new StateMachine(this);
        try {
            serverBinding = new HandleRPC(this);
        }
        catch (RemoteException e) {
            SysLog.logger.severe("An issue occurred while creating the binding to the given RMI address/port: " +
                    e.getMessage());
            //e.printStackTrace();
        }
        this.ticketPool = ServerProperties.getIntialTicketPool();
        SysLog.logger.finer("Created new server with server ID " + this.serverId);
    }

    //endregion

    //region Getters/Setters

    public ReentrantLock getTicketPoolLock() {
        return this.ticketPoolLock;
    }

    public int getServerId() {
        return this.serverId;
    }

    public StateMachine getStateMachine() {
        return this.stateMachine;
    }

    public int getTicketPool() {
        return this.ticketPool;
    }

    public void setTicketPool(int ticketPool) {
        this.ticketPool = ticketPool;
    }

    //endregion

    //region Public Methods

    public int initializeServer() {
        SysLog.logger.finest("Entering method");

        // Register server with RMI, use base port + id as port value
        try {
            this.serverBinding.setupConnection();
            SysLog.logger.info("Successfully bound to the specified RMI address/port");
        }
        catch (RemoteException e) {
            SysLog.logger.severe("An issue occurred while binding to the given RMI address/port: " + e.getMessage());
            e.printStackTrace();
            SysLog.logger.finest("Exiting method");
            return 1;
        }

        // Setup state to be a follower
        synchronized (this.stateMachine.getCurrentState()) {
            this.stateMachine.setCurrentState(StateType.FOLLOWER);
            SysLog.logger.info("Set state machine role to FOLLOWER");
        }

        SysLog.logger.finest("Exiting method");
        return 0;
    }

    // Wait for the state machine to finish execution and exit
    public void waitForExitState() {
        SysLog.logger.finest("Entering method");

        while (true) {
            try {
                this.stateMachine.runMachine();
            }
            catch (RemoteException e) {
                SysLog.logger.severe("An issue occurred while running the state machine: " + e.getMessage());
                e.printStackTrace();
                SysLog.logger.finest("Exiting method");
                return;
            }
        }

        //SysLog.logger.fine("Exiting method");
    }

    //endregion
}