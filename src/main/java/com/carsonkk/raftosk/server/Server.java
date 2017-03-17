package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.SysFiles;
import main.java.com.carsonkk.raftosk.global.SysLog;

import java.rmi.RemoteException;
import java.util.concurrent.locks.ReentrantLock;

// Top-level server functionality
public class Server {
    //region Private members

    private final ReentrantLock ticketPoolLock = new ReentrantLock();

    private int serverId;
    private StateMachine stateMachine;
    private HandleRPC serverBinding;
    private int ticketPool;
    private boolean offline;

    //endregion

    //region Constructors

    public Server(int serverId, boolean offline) {
        this.serverId = serverId;
        this.offline = offline;
        this.stateMachine = new StateMachine(this);
        try {
            serverBinding = new HandleRPC(this);
        }
        catch (RemoteException e) {
            SysLog.logger.severe("An issue occurred while creating the binding to the given RMI address/port: " +
                    e.getMessage());
        }
        this.ticketPool = SysFiles.getIntialTicketPool();
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
        this.stateMachine.getCurrentStateLock().lock();
        try
        {
            if(this.offline) {
                this.stateMachine.setCurrentState(StateType.OFFLINE);
                SysLog.logger.info("Set state machine role to OFFLINE");
            }
            else
            {
                this.stateMachine.setCurrentState(StateType.FOLLOWER);
                SysLog.logger.info("Set state machine role to FOLLOWER");
            }
        }
        finally {
            this.stateMachine.getCurrentStateLock().unlock();
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
    }

    //endregion
}