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
        SysLog.logger.finer("Created new RPC handler");
    }

    public HandleRPC(Server server, RPCType rpc, RPCInterface remoteServer) throws RemoteException {
        this(server);
        this.rpc = rpc;
        this.remoteServer = remoteServer;
        SysLog.logger.finer("Created new RPC handler with RPC " + this.rpc);
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
        SysLog.logger.finest("Entering method");

        RPCInterface remoteLeader;
        RPCInterface remoteFollower;
        ReturnValueRPC returnValueRPC = new ReturnValueRPC();

        this.server.getStateMachine().getCurrentStateLock().lock();
        this.server.getStateMachine().getLeaderIdLock().lock();
        this.server.getStateMachine().getLogLock().lock();
        try {
            // Check if this is the leader server
            if(this.server.getStateMachine().getCurrentState() == StateType.LEADER) {
                switch(command.getCommandType()) {
                    case BUY: {
                        SysLog.logger.info("Received BUY command for " + command.getTicketAmount() + " tickets");

                        this.server.getTicketPoolLock().lock();
                        try {
                            if(command.getTicketAmount() <= this.server.getTicketPool()) {
                                // Update the ticket pool
                                this.server.setTicketPool(this.server.getTicketPool() - command.getTicketAmount());

                                // Update the local log and state machine
                                int index = this.server.getStateMachine().getLastLogIndex() + 1;
                                LogEntry logEntry = new LogEntry(index, this.server.getStateMachine().getCurrentTerm(),
                                        command);
                                this.server.getStateMachine().getLog().add(logEntry);
                                this.server.getStateMachine().setPrevLogIndex(this.server.getStateMachine().getLastLogIndex());
                                this.server.getStateMachine().setPrevLogTerm(this.server.getStateMachine().getLastLogTerm());
                                this.server.getStateMachine().setLastLogIndex(index);
                                this.server.getStateMachine().setLastLogTerm(this.server.getStateMachine().getCurrentTerm());

                                // Send out AppendEntriesRPC with new entry
                                for (int i = 1; i < ServerProperties.getMaxServerCount() + 1; i++) {
                                    // Don't send vote for this server
                                    if (i != this.server.getServerId()) {
                                        // Connect to the server
                                        remoteFollower = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                                ServerProperties.getBaseServerPort() + i, true);
                                        if (remoteFollower == null) {
                                            SysLog.logger.info("Server " + i +
                                                    " is currently offline, couldn't send append entry RPC");
                                            continue;
                                        }

                                        // Submit blocking RPC call
                                        returnValueRPC = remoteFollower.appendEntriesRPC(this.server.getServerId(),
                                                this.server.getStateMachine().getCurrentTerm(),
                                                this.server.getStateMachine().getPrevLogIndex(),
                                                this.server.getStateMachine().getPrevLogTerm(),
                                                this.server.getStateMachine().getLog(),
                                                this.server.getStateMachine().getCommitIndex());
                                    }

                                }

                                // Set the return value
                                returnValueRPC.setValue(this.server.getTicketPool());
                                returnValueRPC.setCondition(true);
                                SysLog.logger.info("Successfully sold " + command.getTicketAmount() + " tickets");
                            }
                            else
                            {
                                returnValueRPC.setValue(this.server.getTicketPool());
                                returnValueRPC.setCondition(false);
                                SysLog.logger.info("Invalid request for " + command.getTicketAmount() +
                                        " tickets (only " + this.server.getTicketPool() + " left)");
                            }
                        }
                        finally {
                            this.server.getTicketPoolLock().unlock();
                        }
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
                        SysLog.logger.severe("An invalid CommandType was received by the server: " +
                                command.getCommandType());
                        break;
                    }
                }
            }
            else {
                // If leader exists, forward and block, otherwise return a "retry" ReturnValueRPC
                if(this.server.getStateMachine().getLeaderId() != -1) {
                    remoteLeader = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                            ServerProperties.getBaseServerPort() + this.server.getStateMachine().getLeaderId(), true);
                    if(remoteLeader != null) {
                        SysLog.logger.info("Forwarding command from " + this.server.getStateMachine().getCurrentState() +
                                " Server " + this.server.getServerId() + " to LEADER Server " +
                                this.server.getStateMachine().getLeaderId());
                        returnValueRPC = remoteLeader.submitCommandRPC(command);
                    }
                    else {
                        returnValueRPC.setValue(-1);
                        returnValueRPC.setCondition(false);
                    }
                }
                else {
                    returnValueRPC.setValue(-1);
                    returnValueRPC.setCondition(false);
                }
            }
        }
        finally {
            this.server.getStateMachine().getLogLock().unlock();
            this.server.getStateMachine().getCurrentStateLock().unlock();
            this.server.getStateMachine().getLeaderIdLock().unlock();
        }

        SysLog.logger.finest("Exiting method");
        return returnValueRPC;
    }

    // Handle a request for a vote on a new term election for system leader
    public ReturnValueRPC requestVoteRPC(int candidateId, int candidateTerm, int lastLogIndex, int lastLogTerm)
            throws RemoteException {
        SysLog.logger.finest("Entering method");

        ReturnValueRPC ret = new ReturnValueRPC();
        boolean setToFollower = false;

        this.server.getStateMachine().getCurrentStateLock().lock();
        this.server.getStateMachine().getCurrentTermLock().lock();
        this.server.getStateMachine().getVotedForLock().lock();
        this.server.getStateMachine().getTimeoutlock().lock();
        try {
            // Received a request from a candidate in a lower term, tell it to step down
            if(this.server.getStateMachine().getCurrentTerm() > candidateTerm) {
                SysLog.logger.info("Received vote request from candidate with smaller term (candidate: " + candidateTerm +
                        ", this: " + this.server.getStateMachine().getCurrentTerm() + "), " +
                        "set step-down values for candidate and continue");
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                ret.setCondition(false);
            }
            // Received a request from a candidate with a less complete log, tell it to step down
            else if(this.server.getStateMachine().getLastLogTerm() > lastLogTerm ||
                    (this.server.getStateMachine().getLastLogTerm() == lastLogTerm &&
                            this.server.getStateMachine().getLastLogIndex() > lastLogIndex)) {
                SysLog.logger.info("Received vote request from candidate with less complete log (candidate log index/term: "
                        + lastLogIndex + "/" + lastLogTerm + ", this log index/term: " +
                        this.server.getStateMachine().getLastLogIndex() + "/" + this.server.getStateMachine().getLastLogTerm() +
                        "), set step-down values for candidate and continue");
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                ret.setCondition(false);
            }
            // Received a request from a candidate with >= term than this, and this server hasn't voted yet or previously
            // voted for this candidate, and the logs of each are consistent, give it this server's vote
            else if(this.server.getStateMachine().getCurrentTerm() <= candidateTerm &&
                    (this.server.getStateMachine().getVotedFor() == -1 ||
                            this.server.getStateMachine().getVotedFor() == candidateId) &&
                    this.server.getStateMachine().getLastLogIndex() <= lastLogIndex &&
                    this.server.getStateMachine().getLastLogTerm() <= lastLogTerm) {
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                ret.setCondition(true);

                this.server.getStateMachine().setVotedFor(candidateId);

                SysLog.logger.info("Received valid vote request, voted for server " + candidateId + " for term " +
                        candidateTerm);
            }
            // Received a request from a candidate in a greater term that didn't match the voting credentials, update this
            // server's term and tell it the vote request failed
            else if(this.server.getStateMachine().getCurrentTerm() < candidateTerm) {
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                ret.setCondition(false);

                SysLog.logger.info("Received vote request from candidate with higher term (candidate: " + candidateTerm +
                        ", this: " + this.server.getStateMachine().getCurrentTerm() + "), " +
                        "set old-term values for candidate and step down");
                if(this.server.getStateMachine().getCurrentState() == StateType.CANDIDATE ||
                        this.server.getStateMachine().getCurrentState() == StateType.LEADER) {
                    setToFollower = true;
                }
                this.server.getStateMachine().setCurrentTerm(candidateTerm);
                this.server.getStateMachine().setVotedFor(-1);
            }
            // Received a request from a candidate with == term with this server, but this server has already voted for
            // itself, tell the candidate that the vote request failed
            else if(this.server.getStateMachine().getCurrentTerm() == candidateTerm &&
                    this.server.getStateMachine().getVotedFor() == this.server.getServerId()) {
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                ret.setCondition(false);

                SysLog.logger.info("Received vote request from candidate with equal term (candidate: " + candidateTerm +
                        ", this: " + this.server.getStateMachine().getCurrentTerm() + "), " +
                        "set step-down values for candidate and continue");
            }
            // Unknown state
            else {
                System.out.println(this.server.getStateMachine().getCurrentTerm());
                System.out.println(candidateTerm);
                System.out.println(this.server.getStateMachine().getVotedFor());
                System.out.println(this.server.getStateMachine().getLastLogIndex());
                System.out.println(lastLogIndex);
                System.out.println(this.server.getStateMachine().getLastLogTerm());
                System.out.println(lastLogTerm);
                System.out.println(this.server.getStateMachine().getCurrentState());
            }

            // Signal if this server is in the FOLLOWER state
            if(this.server.getStateMachine().getCurrentState() == StateType.FOLLOWER) {
                SysLog.logger.info("Signalling timeout condition in state machine");
                this.server.getStateMachine().getTimeoutCondition().signal();
            }

            // Update this server to be a follower after a step-down occurred (avoid wrongful signaling)
            if(setToFollower) {
                this.server.getStateMachine().setCurrentState(StateType.FOLLOWER);
            }
        }
        finally {
            this.server.getStateMachine().getTimeoutlock().unlock();
            this.server.getStateMachine().getVotedForLock().unlock();
            this.server.getStateMachine().getCurrentTermLock().unlock();
            this.server.getStateMachine().getCurrentStateLock().unlock();
        }

        SysLog.logger.finest("Exiting method");
        return ret;
    }

    // Handle appending entries to the log/recognizing heartbeats
    public ReturnValueRPC appendEntriesRPC(int leaderId, int leaderTerm, int prevLogIndex, int prevLogTerm,
                                           List<LogEntry> log, int lastCommitIndex) throws RemoteException {
        SysLog.logger.finest("Entering method");

        ReturnValueRPC ret = new ReturnValueRPC();
        boolean setToFollower = false;

        this.server.getStateMachine().getCurrentStateLock().lock();
        this.server.getStateMachine().getCurrentTermLock().lock();
        this.server.getStateMachine().getVotedForLock().lock();
        this.server.getStateMachine().getLeaderIdLock().lock();
        this.server.getStateMachine().getTimeoutlock().lock();
        try {
            // The leader's term is less than this server's term, set values to tell leader to step-down
            if(leaderTerm < this.server.getStateMachine().getCurrentTerm()) {
                this.server.getStateMachine().setLeaderId(-1);
                ret.setCondition(false);
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
            }
            // The leader is ahead of this server's term, update this server's term and step down if a candidate/follower
            else if(leaderTerm > this.server.getStateMachine().getCurrentTerm()){
                this.server.getStateMachine().setLeaderId(leaderId);
                ret.setCondition(false);
                ret.setValue(this.server.getStateMachine().getCurrentTerm());
                this.server.getStateMachine().setCurrentTerm(leaderTerm);
                if(this.server.getStateMachine().getCurrentState() == StateType.CANDIDATE ||
                        this.server.getStateMachine().getCurrentState() == StateType.LEADER) {
                    setToFollower = true;
                }
            }
            // Terms match up, compare logs for enough consistency to append
            else {
                // Prev values match, append entry and advance state machine
                if(this.server.getStateMachine().getLog().get(prevLogIndex).getTerm() == prevLogTerm) {
                    LogEntry entry = log.get(prevLogIndex + 1);
                    this.server.getStateMachine().getLog().add(entry);
                    this.server.getStateMachine().setPrevLogIndex(prevLogIndex);
                    this.server.getStateMachine().setPrevLogTerm(prevLogTerm);
                    this.server.getStateMachine().setLastLogIndex(entry.getIndex());
                    this.server.getStateMachine().setLastLogTerm(entry.getTerm());
                }
            }

            if(entries == null) {
                this.server.getStateMachine().setVotedFor(-1);
                SysLog.logger.fine("Heartbeat signal received from leader (server " + leaderId + ")");
            }

            // Signal if this server is in the FOLLOWER state
            if(this.server.getStateMachine().getCurrentState() == StateType.FOLLOWER) {
                this.server.getStateMachine().getTimeoutCondition().signal();
                if(entries == null) {
                    SysLog.logger.fine("Signalling timeout condition in state machine");
                }
                else {
                    SysLog.logger.info("Signalling timeout condition in state machine");
                }
            }

            // Update this server to be a follower after a step-down occurred (avoid wrongful signaling)
            if(setToFollower) {
                this.server.getStateMachine().setCurrentState(StateType.FOLLOWER);
            }
        }
        finally {
            this.server.getStateMachine().getTimeoutlock().unlock();
            this.server.getStateMachine().getLeaderIdLock().unlock();
            this.server.getStateMachine().getVotedForLock().unlock();
            this.server.getStateMachine().getCurrentTermLock().unlock();
            this.server.getStateMachine().getCurrentStateLock().unlock();
        }

        SysLog.logger.finest("Exiting method");
        return ret;
    }

    // Initialize the RMI with the server info
    public boolean setupConnection() throws RemoteException {
        SysLog.logger.finest("Entering method");

        Registry reg = LocateRegistry.createRegistry(ServerProperties.getBaseServerPort() + this.server.getServerId());
        reg.rebind("RPCInterface", this);
        SysLog.logger.info("Server setup complete, ready for connections");

        SysLog.logger.finest("Exiting method");
        return true;
    }

    // Point-of-entry for a HandleRPC thread
    @Override
    public ReturnValueRPC call() throws RemoteException {
        SysLog.logger.finest("Entering method");

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
                if(this.entries == null) {
                    SysLog.logger.fine("Sending APPENDENTRIES RPC to remote server");
                }
                else {
                    SysLog.logger.info("Sending APPENDENTRIES RPC to remote server");
                }
                ret = this.remoteServer.appendEntriesRPC(this.leaderId, this.leaderTerm, this.prevLogIndex, this.prevLogTerm,
                        this.entries, this.lastCommitIndex);
                break;
            }
            default: {
                SysLog.logger.severe("An invalid RPCType was received by the server: " + this.rpc);
                break;
            }
        }

        SysLog.logger.finest("Exiting method");
        return ret;
    }

    //endregion
}
