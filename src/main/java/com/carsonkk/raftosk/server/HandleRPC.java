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

        if(this.server.getStateMachine().getCurrentTerm() == candidateTerm &&
                (this.server.getStateMachine().getVotedFor() == -1 ||
                        this.server.getStateMachine().getVotedFor() == candidateId) &&
                this.server.getStateMachine().getLastLogIndex() <= lastLogIndex &&
                this.server.getStateMachine().getLastLogTerm() <= lastLogTerm) {

            ret.setValue(this.server.getStateMachine().getCurrentTerm());
            ret.setCondition(true);
            this.server.getStateMachine().setVotedFor(candidateId);
            SysLog.logger.info("Received matching term vote request, voted for server " + candidateId + " for term " +
                    candidateTerm);
        }
        else if(this.server.getStateMachine().getCurrentTerm() < candidateTerm) {
            ret.setValue(this.server.getStateMachine().getCurrentTerm());
            ret.setCondition(false);
            this.server.getStateMachine().setCurrentTerm(candidateTerm);
            SysLog.logger.info("Received vote request with larger term, updated local term and abstained from vote");
        }
        else if(this.server.getStateMachine().getCurrentTerm() > candidateTerm) {
            ret.setValue(this.server.getStateMachine().getCurrentTerm());
            ret.setCondition(false);
            SysLog.logger.info("Received vote request from candidate with smaller term, set step-down values");
        }

        this.server.getStateMachine().getTimeoutCondition().signal();
        SysLog.logger.info("Signalled timeout condition in state machine");
        SysLog.logger.fine("Exiting method");
        return ret;
    }

    // Handle appending entries to the log/recognizing heartbeats
    public ReturnValueRPC appendEntriesRPC(int leaderId, int leaderTerm, int prevLogIndex, int prevLogTerm,
                                           List<LogEntry> entries, int lastCommitIndex) throws RemoteException {
        SysLog.logger.fine("Entering method");

        ReturnValueRPC ret = new ReturnValueRPC();

        if(leaderTerm < this.server.getStateMachine().getCurrentTerm()) {
            ret.setCondition(false);
        }
        else {
            ret.setCondition(true);
        }

        ret.setValue(this.server.getStateMachine().getCurrentTerm());
        this.server.getStateMachine().getTimeoutCondition().signal();
        SysLog.logger.info("Signalled timeout condition in state machine");
        SysLog.logger.fine("Exiting method");
        return ret;
    }

    // Initialize the RMI with the server info
    public boolean setupConnection() throws RemoteException {
        SysLog.logger.fine("Entering method");

        try
        {
            Registry reg = LocateRegistry.createRegistry(ServerProperties.getBaseServerPort() +
                    this.server.getServerId());
            reg.rebind("RPCInterface", this);
            SysLog.logger.info("Server setup complete, ready for connections");
        } catch (Exception e) {
            SysLog.logger.severe("An issue occurred while the server was initializing its RMI: " + e.getMessage());
            e.printStackTrace();
            SysLog.logger.fine("Exiting method");
            return false;
        }

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
                SysLog.logger.info("Received SUBMITCOMMAND rpc");
                ret = submitCommandRPC(this.command);
                break;
            }
            case REQUESTVOTE: {
                SysLog.logger.info("Received REQUESTVOTE rpc");
                ret = this.remoteServer.requestVoteRPC(this.candidateId, this.candidateTerm, this.lastLogIndex,
                        this.lastLogTerm);
                break;
            }
            case APPENDENTRIES: {
                SysLog.logger.info("Received APPENDENTRIES rpc");
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
