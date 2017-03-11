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
    }

    public HandleRPC(Server server, RPCType rpc, RPCInterface remoteServer, int candidateId, int candidateTerm, int lastLogIndex,
                     int lastLogTerm) throws RemoteException {
        this(server, rpc, remoteServer);
        this.candidateId = candidateId;
        this.candidateTerm = candidateTerm;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
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
    }

    //endregion

    //region Public Methods

    public ReturnValueRPC submitCommandRPC(Command command) throws RemoteException {
        switch(command.getCommandType()) {
            case BUY: {

                break;
            }
            case SHOW: {

                break;
            }
            case CHANGE: {

                break;
            }
            default: {
                System.out.println("[ERROR] An invalid command was received by the sever");
                break;
            }
        }
        return null;
    }

    public ReturnValueRPC requestVoteRPC(int candidateId, int candidateTerm, int lastLogIndex, int lastLogTerm) throws RemoteException {
        ReturnValueRPC ret = new ReturnValueRPC();

        if(this.server.getStateMachine().getCurrentTerm() == candidateTerm &&
                (this.server.getStateMachine().getVotedFor() == -1 || this.server.getStateMachine().getVotedFor() == candidateId) &&
                this.server.getStateMachine().getLastLogIndex() <= lastLogIndex &&
                this.server.getStateMachine().getLastLogTerm() <= lastLogTerm) {
            ret.setValue(this.server.getStateMachine().getCurrentTerm());
            ret.setCondition(true);
            this.server.getStateMachine().setVotedFor(candidateId);
        }
        else if(this.server.getStateMachine().getCurrentTerm() < candidateTerm) {
            ret.setValue(this.server.getStateMachine().getCurrentTerm());
            ret.setCondition(false);
            this.server.getStateMachine().setCurrentTerm(candidateTerm);
        }
        else if(this.server.getStateMachine().getCurrentTerm() > candidateTerm) {
            ret.setValue(this.server.getStateMachine().getCurrentTerm());
            ret.setCondition(false);
        }

        this.server.getStateMachine().getTimeoutCondition().signal();
        return ret;
    }

    public ReturnValueRPC appendEntriesRPC(int leaderId, int leaderTerm, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int lastCommitIndex)
            throws RemoteException {
        ReturnValueRPC ret = new ReturnValueRPC();

        if(leaderTerm < this.server.getStateMachine().getCurrentTerm()) {
            ret.setCondition(false);
        }
        else {
            ret.setCondition(true);
        }

        ret.setValue(this.server.getStateMachine().getCurrentTerm());
        this.server.getStateMachine().getTimeoutCondition().signal();
        return ret;
    }

    public boolean setupConnection() throws RemoteException {
        try
        {
            Registry reg = LocateRegistry.createRegistry(ServerProperties.getBaseServerPort() + this.server.getServerId());
            reg.rebind("RPCInterface", this);
            System.out.println("[LOG-1] Server ready...");
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while the server was initializing its RMI: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public ReturnValueRPC call() throws RemoteException {
        ReturnValueRPC ret = null;

        switch(this.rpc) {
            case SUBMITCOMMAND: {
                ret = submitCommandRPC(this.command);
                break;
            }
            case REQUESTVOTE: {
                ret = this.remoteServer.requestVoteRPC(this.candidateId, this.candidateTerm, this.lastLogIndex, this.lastLogTerm);
                break;
            }
            case APPENDENTRIES: {
                ret = this.remoteServer.appendEntriesRPC(this.leaderId, this.leaderTerm, this.prevLogIndex, this.prevLogTerm,
                        this.entries, this.lastCommitIndex);
                break;
            }
            default: {
                System.out.println("[ERROR] An invalid RPCType was set when trying to execute a HandleRPC thread");
                break;
            }
        }

        return ret;
    }

    //endregion
}
