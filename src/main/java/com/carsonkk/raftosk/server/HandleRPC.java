package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.Callable;

// Multiplex between various RPC calls, handle all RMI-related setup/teardown
public class HandleRPC extends UnicastRemoteObject implements RPCInterface, Callable<ReturnValueRPC>{
    //region Private Members

    private Server server;
    private RPCType rpc;
    private RPCInterface remoteServer;

    //submitCommandRPC parameter storage
    private Command command;

    //requestVoteRPC parameter storage
    private int candidateId;
    private int candidateTerm;
    private int lastLogIndex;
    private int lastLogTerm;

    //appendEntriesRPC parameter storage
    private int leaderId;
    private int leaderTerm;
    private int prevLogIndex;
    private int prevLogTerm;
    private List<LogEntry> entries;
    private int lastCommitIndex;

    //endregion

    //region Constructors

    public HandleRPC(Server server) throws RemoteException {
        this.server = server;
        SysLog.logger.finer("Created new RPC handler with server " + this.server);
    }

    public HandleRPC(Server server, RPCType rpc, RPCInterface remoteServer) throws RemoteException {
        this(server);
        this.rpc = rpc;
        this.remoteServer = remoteServer;
        SysLog.logger.finer("Created new RPC handler with RPC " + this.rpc + " and remote server " + this.remoteServer);
    }

    public HandleRPC(Server server, RPCType rpc, RPCInterface remoteServer, Command command) throws RemoteException {
        this(server, rpc, remoteServer);
        this.command = command;
        SysLog.logger.finer("Created new RPC handler with command " + this.command);
    }

    public HandleRPC(Server server, RPCType rpc, RPCInterface remoteServer, int candidateId, int candidateTerm,
                     int lastLogIndex, int lastLogTerm) throws RemoteException {
        this(server, rpc, remoteServer);
        this.candidateId = candidateId;
        this.candidateTerm = candidateTerm;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
        SysLog.logger.finer("Created new RPC handler with candidate ID " + this.candidateId + ", candidate term " +
                this.candidateTerm + ", last log index " + this.lastLogIndex + ", and last log term " + this.lastLogTerm);
    }

    public HandleRPC(Server server, RPCType rpc, RPCInterface remoteServer, int leaderId, int leaderTerm, int prevLogIndex,
                     int prevLogTerm, List<LogEntry> entries, int lastCommitIndex) throws RemoteException {
        this(server, rpc, remoteServer);
        this.leaderId = leaderId;
        this.leaderTerm = leaderTerm;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.lastCommitIndex = lastCommitIndex;
        SysLog.logger.finer("Created new RPC handler with leader ID " + this.leaderId + ", leader term " +
                this.leaderTerm + ", prev log index " + this.prevLogIndex + ", prev log term " + this.prevLogTerm +
                ", entries " + this.entries + ", and last commit index " + this.lastCommitIndex);
    }

    //endregion

    //region Public Methods

    // Handle commands sent to the server by a client
    public ReturnValueRPC submitCommandRPC(Command command) throws RemoteException {
        SysLog.logger.fine("Entering method");

        switch(command.getCommandType()) {
            case BUY: {
                SysLog.logger.info("Received BUY command");

                break;
            }
            case SHOW: {
                SysLog.logger.info("Received SHOW command");

                break;
            }
            case CHANGE: {
                SysLog.logger.info("Received CHANGE command");

                break;
            }
            default: {
                SysLog.logger.severe("An invalid CommandType was received by the server: " + command.getCommandType());
                break;
            }
        }

        SysLog.logger.fine("Exiting method");
        return null;
    }

    // Handle a request for a vote on a new term election for system leader
    public ReturnValueRPC requestVoteRPC(int candidateId, int candidateTerm, int lastLogIndex, int lastLogTerm)
            throws RemoteException {
        SysLog.logger.fine("Entering method");

        ReturnValueRPC ret = new ReturnValueRPC();

        this.server.getStateMachine().getCurrentStateLock().lock();
        this.server.getStateMachine().getCurrentTermLock().lock();
        this.server.getStateMachine().getVotedForLock().lock();
        this.server.getStateMachine().getTimeoutlock().lock();
        try {
            if(this.server.getStateMachine().getCurrentTerm() > candidateTerm) {
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                ret.setCondition(false);
                SysLog.logger.info("Received vote request from candidate with smaller term (candidate: " + candidateTerm +
                        ", this: " + this.server.getStateMachine().getCurrentTerm() + "), set step-down values");
            }
            else if(this.server.getStateMachine().getCurrentTerm() <= candidateTerm &&
                    (this.server.getStateMachine().getVotedFor() == -1 ||
                            this.server.getStateMachine().getVotedFor() == candidateId) &&
                    this.server.getStateMachine().getLastLogIndex() <= lastLogIndex &&
                    this.server.getStateMachine().getLastLogTerm() <= lastLogTerm) {
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                ret.setCondition(true);

                this.server.getStateMachine().setVotedFor(candidateId);

                SysLog.logger.info("Received valid term vote request, voted for server " + candidateId + " for term " +
                        candidateTerm);
            }
            else {
                System.out.println("a");
                //this.server.getStateMachine().setVotedFor(-1);
            }

            if(this.server.getStateMachine().getCurrentState() == StateType.FOLLOWER) {
                SysLog.logger.info("Signalling timeout condition in state machine");
                this.server.getStateMachine().getTimeoutCondition().signal();
            }
        }
        finally {
            this.server.getStateMachine().getTimeoutlock().unlock();
            this.server.getStateMachine().getVotedForLock().unlock();
            this.server.getStateMachine().getCurrentTermLock().unlock();
            this.server.getStateMachine().getCurrentStateLock().unlock();
        }

        SysLog.logger.fine("Exiting method");
        return ret;
    }

    // Handle appending entries to the log/recognizing heartbeats
    public ReturnValueRPC appendEntriesRPC(int leaderId, int leaderTerm, int prevLogIndex, int prevLogTerm,
                                           List<LogEntry> entries, int lastCommitIndex) throws RemoteException {
        boolean heartbeat = false;

        if(entries == null) {
            heartbeat = true;
        }

        if(heartbeat) {
            SysLog.logger.finest("Entering method");
        }
        else {
            SysLog.logger.fine("Entering method");
        }

        ReturnValueRPC ret = new ReturnValueRPC();

        this.server.getStateMachine().getCurrentStateLock().lock();
        this.server.getStateMachine().getCurrentTermLock().lock();
        this.server.getStateMachine().getVotedForLock().lock();
        this.server.getStateMachine().getTimeoutlock().lock();
        try {
            if(leaderTerm < this.server.getStateMachine().getCurrentTerm()) {
                ret.setCondition(false);
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
            }
            else {
                ret.setCondition(true);
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                this.server.getStateMachine().setCurrentTerm(leaderTerm);
            }

            if(heartbeat) {
                this.server.getStateMachine().setVotedFor(-1);
                SysLog.logger.finest("Heartbeat signal received from leader (server " + leaderId + ")");
            }

            if(this.server.getStateMachine().getCurrentState() == StateType.FOLLOWER) {
                this.server.getStateMachine().getTimeoutCondition().signal();
                if(heartbeat) {
                    SysLog.logger.finest("Signalling timeout condition in state machine");
                }
                else {
                    SysLog.logger.fine("Signalling timeout condition in state machine");
                }
            }
        }
        finally {
            this.server.getStateMachine().getTimeoutlock().unlock();
            this.server.getStateMachine().getVotedForLock().unlock();
            this.server.getStateMachine().getCurrentTermLock().unlock();
            this.server.getStateMachine().getCurrentStateLock().unlock();
        }

        if(heartbeat) {
            SysLog.logger.finest("Exiting method");
        }
        else {
            SysLog.logger.fine("Exiting method");
        }
        return ret;
    }

    // Initialize the RMI with the server info
    public boolean setupConnection() throws RemoteException {
        SysLog.logger.fine("Entering method");

        Registry reg = LocateRegistry.createRegistry(ServerProperties.getBaseServerPort() + this.server.getServerId());
        reg.rebind("RPCInterface", this);
        SysLog.logger.info("Server setup complete, ready for connections");

        SysLog.logger.fine("Exiting method");
        return true;
    }

    // Point-of-entry for a HandleRPC thread
    @Override
    public ReturnValueRPC call() throws RemoteException {
        SysLog.logger.fine("Entering method");

        ReturnValueRPC ret = null;

        switch(this.rpc) {
            case SUBMITCOMMAND: {
                SysLog.logger.info("Sending SUBMITCOMMAND RPC to remote server");
                ret = submitCommandRPC(this.command);
                break;
            }
            case REQUESTVOTE: {
                SysLog.logger.info("Sending REQUESTVOTE RPC to remote server");
                ret = this.remoteServer.requestVoteRPC(this.candidateId, this.candidateTerm, this.lastLogIndex,
                        this.lastLogTerm);
                break;
            }
            case APPENDENTRIES: {
                SysLog.logger.info("Sending APPENDENTRIES RPC to remote server");
                ret = this.remoteServer.appendEntriesRPC(this.leaderId, this.leaderTerm, this.prevLogIndex, this.prevLogTerm,
                        this.entries, this.lastCommitIndex);
                break;
            }
            default: {
                SysLog.logger.severe("An invalid RPCType was received by the server: " + this.rpc);
                break;
            }
        }

        SysLog.logger.fine("Exiting method");
        return ret;
    }

    //endregion
}
