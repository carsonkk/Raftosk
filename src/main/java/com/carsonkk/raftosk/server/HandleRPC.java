package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
    private List<LogEntry> log;
    private int commitIndex;
    private List<Integer> nextIndexes;

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
                     int prevLogTerm, List<LogEntry> log, int commitIndex, List<Integer> nextIndexes) throws RemoteException {
        this(server, rpc, remoteServer);
        this.leaderId = leaderId;
        this.leaderTerm = leaderTerm;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.log = log;
        this.commitIndex = commitIndex;
        this.nextIndexes = nextIndexes;
        SysLog.logger.finer("Created new RPC handler with leader ID " + this.leaderId + ", leader term " +
                this.leaderTerm + ", prev log index " + this.prevLogIndex + ", prev log term " + this.prevLogTerm +
                ", log " + this.log + ", and commit index " + this.commitIndex);
    }

    //endregion

    //region Public Methods

    // Get the current state of the server
    public ReturnValueRPC getStateRPC() throws RemoteException {
        SysLog.logger.finest("Entering method");
        ReturnValueRPC returnValueRPC = new ReturnValueRPC();

        this.server.getStateMachine().getCurrentStateLock().lock();
        try {
            switch (this.server.getStateMachine().getCurrentState()) {
                case OFFLINE: {
                    returnValueRPC.setValue(0);
                    break;
                }
                case FOLLOWER: {
                    returnValueRPC.setValue(1);
                    break;
                }
                case CANDIDATE: {
                    returnValueRPC.setValue(2);
                    break;
                }
                case LEADER: {
                    returnValueRPC.setValue(3);
                    break;
                }
            }
        }
        finally {
            this.server.getStateMachine().getCurrentStateLock().unlock();
        }

        SysLog.logger.finest("Exiting method");
        return returnValueRPC;
    }


    // Handle commands sent to the server by a client
    public ReturnValueRPC submitCommandRPC(Command command) throws RemoteException {
        SysLog.logger.finest("Entering method");
        ReturnValueRPC returnValueRPC = new ReturnValueRPC();

        this.server.getStateMachine().getAppendEntriesLock().lock();
        try {
            this.server.getStateMachine().getCurrentStateLock().lock();
            this.server.getStateMachine().getLeaderIdLock().lock();
            this.server.getStateMachine().getLogLock().lock();
            try {
                int successfulAppends = 0;
                boolean exitWithoutUpdate = false;
                List<FutureTask<ReturnValueRPC>> futureTaskList = new ArrayList<>();
                Iterator<FutureTask<ReturnValueRPC>> futureTaskIterator;
                Callable<ReturnValueRPC> callable;
                RPCInterface remoteServerToUpdate;
                ReturnValueRPC remoteReturnValueRPC = new ReturnValueRPC();
                RPCInterface remoteLeader;

                // Check if this is the leader server
                if (this.server.getStateMachine().getCurrentState() == StateType.LEADER ||
                        command.getCommandType() == CommandType.SHOW) {
                    // Handle command-specific modifications
                    switch (command.getCommandType()) {
                        case BUY: {
                            SysLog.logger.info("Received BUY command for " + command.getTicketAmount() + " tickets");

                            this.server.getTicketPoolLock().lock();
                            try {
                                if (command.getTicketAmount() <= this.server.getTicketPool()) {
                                    // Update the ticket pool
                                    this.server.setTicketPool(this.server.getTicketPool() - command.getTicketAmount());
                                } else {
                                    returnValueRPC.setServerId(this.server.getServerId());
                                    returnValueRPC.setValue(this.server.getTicketPool());
                                    returnValueRPC.setCondition(false);
                                    exitWithoutUpdate = true;
                                    SysLog.logger.info("Invalid request for " + command.getTicketAmount() +
                                            " tickets (only " + this.server.getTicketPool() + " left)");
                                }
                            } finally {
                                this.server.getTicketPoolLock().unlock();
                            }

                            SysLog.logger.info("Finished BUY command for " + command.getTicketAmount() + " tickets");
                            break;
                        }
                        case SHOW: {
                            SysLog.logger.info("Received SHOW command");
                            SysLog.logger.info("Finished SHOW command");
                            break;
                        }
                        case CHANGE: {
                            SysLog.logger.info("Received CHANGE command");
                            SysLog.logger.info("Finished CHANGE command");
                            break;
                        }
                    }

                    // If successful, update local and remote logs
                    if(!exitWithoutUpdate) {
                        this.server.getTicketPoolLock().lock();
                        this.server.getStateMachine().getNextIndexesLock().lock();
                        this.server.getStateMachine().getCurrentTermLock().lock();
                        this.server.getStateMachine().getLastLogIndexLock().lock();
                        this.server.getStateMachine().getLastLogTermLock().lock();
                        this.server.getStateMachine().getPrevLogIndexLock().lock();
                        this.server.getStateMachine().getPrevLogTermLock().lock();
                        this.server.getStateMachine().getCommitIndexLock().lock();
                        try {
                            // Update the local log and state machine
                            int index = this.server.getStateMachine().getLastLogIndex() + 1;
                            LogEntry logEntry = new LogEntry(index, this.server.getStateMachine().getCurrentTerm(), command);
                            this.server.getStateMachine().getLog().add(logEntry);
                            this.server.getStateMachine().setPrevLogIndex(this.server.getStateMachine().getLastLogIndex());
                            this.server.getStateMachine().setPrevLogTerm(this.server.getStateMachine().getLastLogTerm());
                            this.server.getStateMachine().setLastLogIndex(index);
                            this.server.getStateMachine().setLastLogTerm(this.server.getStateMachine().getCurrentTerm());
                            this.server.getStateMachine().getNextIndexes().set(this.server.getServerId() - 1, index + 1);
                            successfulAppends++;

                            // Send out AppendEntriesRPC with new entry
                            for (int i = 1; i < SysFiles.getMaxServerCount() + 1; i++) {
                                // Don't send vote for this server
                                if (i != this.server.getServerId()) {
                                    // Connect to the server
                                    remoteServerToUpdate = ConnectToServer.connect(SysFiles.getBaseServerAddress(),
                                            SysFiles.getBaseServerPort() + i, true);
                                    if (remoteServerToUpdate == null) {
                                        SysLog.logger.info("Server " + i +
                                                " is currently offline, couldn't send append entry RPC");
                                        continue;
                                    }

                                    callable = new HandleRPC(this.server, RPCType.APPENDENTRIES, remoteServerToUpdate,
                                            this.server.getServerId(), this.server.getStateMachine().getCurrentTerm(),
                                            this.server.getStateMachine().getPrevLogIndex(),
                                            this.server.getStateMachine().getPrevLogTerm(),
                                            this.server.getStateMachine().getLog(),
                                            this.server.getStateMachine().getCommitIndex(),
                                            this.server.getStateMachine().getNextIndexes());
                                    FutureTask<ReturnValueRPC> futureTask = new FutureTask<>(callable);
                                    futureTaskList.add(futureTask);
                                    this.server.getStateMachine().getExecutorService().submit(futureTask);
                                }
                            }

                            // Get a majority of append responses
                            while (successfulAppends < SysFiles.getMajorityVote() &&
                                    remoteReturnValueRPC != null) {
                                try {
                                    futureTaskIterator = futureTaskList.iterator();
                                    while (futureTaskIterator.hasNext()) {
                                        FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                                        if (futureTask.isDone()) {
                                            remoteReturnValueRPC = futureTask.get();
                                            // Received true, either from successful append or heartbeat
                                            if (remoteReturnValueRPC.getCondition()) {
                                                if (remoteReturnValueRPC.getValue() ==
                                                        this.server.getStateMachine().getCurrentTerm()) {
                                                    successfulAppends++;
                                                    this.server.getStateMachine().getNextIndexes().set(
                                                            remoteReturnValueRPC.getServerId() - 1,
                                                            remoteReturnValueRPC.getNextIndex());
                                                } else {
                                                    System.out.println("hi");
                                                }
                                            }
                                            // Received false, either this leader is further ahead or the remote is ahead
                                            else {
                                                if (remoteReturnValueRPC.getValue() >
                                                        this.server.getStateMachine().getCurrentTerm()) {
                                                    returnValueRPC.setValue(-1);
                                                    returnValueRPC.setCondition(false);
                                                    SysLog.logger.info("Received a response from a server with a " +
                                                            "higher term while trying to append a " +
                                                            command.getCommandType() + " command");
                                                    remoteReturnValueRPC = null;
                                                    break;
                                                }
                                                else {
                                                    SysLog.logger.info("Received a response from a server with a " +
                                                            "lower term while trying to append a " +
                                                            command.getCommandType() + " command, lowering next index and " +
                                                            "retrying");
                                                }
                                            }
                                            futureTaskIterator.remove();
                                        }
                                    }
                                } catch (InterruptedException | ExecutionException e) {
                                    SysLog.logger.info("Exception occurred: " + e.getMessage());
                                }
                            }

                            // Succeeded in obtaining majority appends
                            if (successfulAppends >= SysFiles.getMajorityVote()) {
                                SysLog.logger.info("Successfully executed the " + command.getCommandType() + " command");
                                // Set the return value
                                this.server.getStateMachine().setCommitIndex(index);
                                returnValueRPC.setValue(this.server.getTicketPool());
                                returnValueRPC.setCondition(true);
                                if(command.getCommandType() == CommandType.SHOW) {
                                    returnValueRPC.setTicketPool(this.server.getTicketPool());
                                    returnValueRPC.setLog(this.server.getStateMachine().getLog());
                                    returnValueRPC.setCommitIndex(this.server.getStateMachine().getCommitIndex());

                                    // Let leader know
                                    if(this.server.getStateMachine().getCurrentState() != StateType.LEADER) {
                                        remoteServerToUpdate = ConnectToServer.connect(SysFiles.getBaseServerAddress(),
                                                SysFiles.getBaseServerPort() + this.server.getStateMachine().getLeaderId(),
                                                true);
                                        if (remoteServerToUpdate == null) {
                                            SysLog.logger.info("Leader server is currently offline, " +
                                                    "couldn't send append entry RPC");
                                        }
                                        else {
                                            callable = new HandleRPC(this.server, RPCType.APPENDENTRIES, remoteServerToUpdate,
                                                    this.server.getServerId(), this.server.getStateMachine().getCurrentTerm(),
                                                    this.server.getStateMachine().getPrevLogIndex(),
                                                    this.server.getStateMachine().getPrevLogTerm(),
                                                    null,
                                                    this.server.getStateMachine().getCommitIndex(),
                                                    this.server.getStateMachine().getNextIndexes());
                                            FutureTask<ReturnValueRPC> futureTask = new FutureTask<>(callable);
                                            this.server.getStateMachine().getExecutorService().submit(futureTask);
                                            while(!futureTask.isDone()) {}
                                            try {
                                                remoteReturnValueRPC = futureTask.get();
                                                if(remoteReturnValueRPC.getServerId() ==
                                                        this.server.getStateMachine().getLeaderId() &&
                                                        remoteReturnValueRPC.getCondition()) {
                                                    SysLog.logger.info("Updated leader next indexes");
                                                }
                                            }
                                            catch (InterruptedException | ExecutionException e) {
                                                SysLog.logger.info("Exception occurred: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            }

                        }
                        finally {
                            this.server.getTicketPoolLock().unlock();
                            this.server.getStateMachine().getNextIndexesLock().unlock();
                            this.server.getStateMachine().getCurrentTermLock().unlock();
                            this.server.getStateMachine().getLastLogIndexLock().unlock();
                            this.server.getStateMachine().getLastLogTermLock().unlock();
                            this.server.getStateMachine().getPrevLogIndexLock().unlock();
                            this.server.getStateMachine().getPrevLogTermLock().unlock();
                            this.server.getStateMachine().getCommitIndexLock().unlock();
                        }
                    }
                } else {
                    // If leader exists, forward and block, otherwise return a "retry" ReturnValueRPC
                    if (this.server.getStateMachine().getLeaderId() != -1) {
                        remoteLeader = ConnectToServer.connect(SysFiles.getBaseServerAddress(),
                                SysFiles.getBaseServerPort() + this.server.getStateMachine().getLeaderId(), true);
                        if (remoteLeader != null) {
                            SysLog.logger.info("Forwarding command from " + this.server.getStateMachine().getCurrentState() +
                                    " Server " + this.server.getServerId() + " to LEADER Server " +
                                    this.server.getStateMachine().getLeaderId());
                            // Prevent deadlock when handling append entry request from leader
                            this.server.getStateMachine().getLogLock().unlock();
                            this.server.getStateMachine().getCurrentStateLock().unlock();
                            this.server.getStateMachine().getLeaderIdLock().unlock();
                            returnValueRPC = remoteLeader.submitCommandRPC(command);
                            this.server.getStateMachine().getLogLock().lock();
                            this.server.getStateMachine().getCurrentStateLock().lock();
                            this.server.getStateMachine().getLeaderIdLock().lock();
                        } else {
                            returnValueRPC.setValue(-1);
                            returnValueRPC.setCondition(false);
                        }
                    } else {
                        returnValueRPC.setValue(-1);
                        returnValueRPC.setCondition(false);
                    }
                }
            } finally {
                this.server.getStateMachine().getLogLock().unlock();
                this.server.getStateMachine().getCurrentStateLock().unlock();
                this.server.getStateMachine().getLeaderIdLock().unlock();
            }
        }
        finally {
            this.server.getStateMachine().getAppendEntriesLock().unlock();
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
        this.server.getStateMachine().getIsWaitingLock().lock();
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
            if(this.server.getStateMachine().getCurrentState() == StateType.FOLLOWER &&
                    this.server.getStateMachine().getIsWaiting()) {

                SysLog.logger.fine("Signalling timeout condition in state machine");
                this.server.getStateMachine().getTimeoutLock().lock();
                try {
                    this.server.getStateMachine().getTimeoutCondition().signal();
                }
                finally {
                    this.server.getStateMachine().getTimeoutLock().unlock();
                }
            }

            // Update this server to be a follower after a step-down occurred (avoid wrongful signaling)
            if(setToFollower) {
                this.server.getStateMachine().setCurrentState(StateType.FOLLOWER);
            }
        }
        finally {
            this.server.getStateMachine().getIsWaitingLock().unlock();
            this.server.getStateMachine().getVotedForLock().unlock();
            this.server.getStateMachine().getCurrentTermLock().unlock();
            this.server.getStateMachine().getCurrentStateLock().unlock();
        }

        SysLog.logger.finest("Exiting method");
        return ret;
    }

    // Handle appending entries to the log/recognizing heartbeats
    public ReturnValueRPC appendEntriesRPC(int leaderId, int leaderTerm, int prevLogIndex, int prevLogTerm,
                                           List<LogEntry> log, int commitIndex, List<Integer> nextIndexes) throws RemoteException {
        SysLog.logger.finest("Entering method");

        ReturnValueRPC ret = new ReturnValueRPC();
        boolean setToFollower = false;

        this.server.getStateMachine().getCurrentStateLock().lock();
        this.server.getStateMachine().getCurrentTermLock().lock();
        this.server.getStateMachine().getVotedForLock().lock();
        this.server.getStateMachine().getCommitIndexLock().lock();
        this.server.getStateMachine().getLeaderIdLock().lock();
        this.server.getStateMachine().getIsWaitingLock().lock();
        this.server.getStateMachine().getNextIndexesLock().lock();
        try {
            // Log is null, heartbeat from leader server (typical case)
            if(log == null) {
                this.server.getStateMachine().setVotedFor(-1);
                for(int i = 0; i < this.server.getStateMachine().getNextIndexes().size(); i++) {
                    if(this.server.getStateMachine().getNextIndexes().get(i) < nextIndexes.get(i)) {
                        this.server.getStateMachine().getNextIndexes().set(i, nextIndexes.get(i));
                    }
                    else if(this.server.getStateMachine().getNextIndexes().get(i) > nextIndexes.get(i)) {
                        ret.getNextIndexes().set(i,this.server.getStateMachine().getNextIndexes().get(i));
                    }
                }

                // Check if a newer leader exists
                if(leaderTerm > this.server.getStateMachine().getCurrentTerm()) {
                    SysLog.logger.fine("Received a heartbeat from a leader with a higher term, stepping down if necessary");

                    this.server.getStateMachine().setLeaderId(leaderId);
                    this.server.getStateMachine().setCurrentTerm(leaderTerm);
                    if(this.server.getStateMachine().getCurrentState() == StateType.CANDIDATE ||
                            this.server.getStateMachine().getCurrentState() == StateType.LEADER) {
                        setToFollower = true;

                        ret.setCondition(false);
                        ret.setValue(this.server.getStateMachine().getCurrentTerm());
                    }
                    else {
                        ret.setValue(-1);
                        ret.setCondition(true);
                        ret.setCommitIndex(this.server.getStateMachine().getCommitIndex());
                    }
                }
                // Update commit index if necessary
                else {
                    if(this.server.getStateMachine().getCommitIndex() < commitIndex) {
                        // Update ticket pool if the command was a buy command
                        this.server.getStateMachine().getLogLock().lock();
                        this.server.getTicketPoolLock().lock();
                        try {
                            if(this.server.getStateMachine().getLog().size() <= commitIndex) {
                                SysLog.logger.warning("Detected commit index change for log entry not present in log");
                            }
                            else {
                                SysLog.logger.info("Updating commit log entry");
                                this.server.getStateMachine().setCommitIndex(commitIndex);
                                Command command = this.server.getStateMachine().getLog().get(commitIndex).getCommand();
                                if(command.getCommandType() == CommandType.BUY) {
                                    SysLog.logger.info("Updating state machine from committed BUY command");
                                    this.server.setTicketPool(this.server.getTicketPool() - command.getTicketAmount());
                                }
                            }
                        }
                        finally {
                            this.server.getStateMachine().getLogLock().unlock();
                            this.server.getTicketPoolLock().unlock();
                        }
                    }

                    this.server.getStateMachine().getLastLogIndexLock().lock();
                    try {
                        ret.setServerId(this.server.getServerId());
                        ret.setNextIndex(this.server.getStateMachine().getLastLogIndex() + 1);
                    }
                    finally {
                        this.server.getStateMachine().getLastLogIndexLock().unlock();
                    }
                    ret.setValue(-1);
                    ret.setCondition(true);
                }

                SysLog.logger.fine("Heartbeat signal received from leader (server " + leaderId + ")");
            }
            else {
                this.server.getStateMachine().getLogLock().lock();
                this.server.getStateMachine().getLastLogIndexLock().lock();
                this.server.getStateMachine().getLastLogTermLock().lock();
                this.server.getStateMachine().getPrevLogIndexLock().lock();
                this.server.getStateMachine().getPrevLogTermLock().lock();
                try {
                    // The leader's term is less than this server's term, set values to tell leader to step-down
                    if(leaderTerm < this.server.getStateMachine().getCurrentTerm()) {
                        SysLog.logger.info("Received append entry request from leader with lower term, telling it to " +
                                "step down");
                        this.server.getStateMachine().setLeaderId(-1);
                        ret.setCondition(false);
                        ret.setValue(this.server.getStateMachine().getCurrentTerm());
                    }
                    // The leader is ahead of this server's term, update this server's term and step down if a candidate/leader
                    else if(leaderTerm > this.server.getStateMachine().getCurrentTerm()){
                        if(log == null) {
                            SysLog.logger.info("Received append entry request from a leader with a higher term, " +
                                    "stepping down if necessary");
                            this.server.getStateMachine().setLeaderId(leaderId);
                            ret.setCondition(false);
                            ret.setValue(this.server.getStateMachine().getCurrentTerm());
                            this.server.getStateMachine().setCurrentTerm(leaderTerm);
                            if(this.server.getStateMachine().getCurrentState() == StateType.CANDIDATE ||
                                    this.server.getStateMachine().getCurrentState() == StateType.LEADER) {
                                setToFollower = true;
                            }
                        }
                        else {
                            SysLog.logger.info("Received append entry request from a leader with a higher term, syncing");
                            this.server.getStateMachine().setLeaderId(leaderId);
                            this.server.getStateMachine().setCurrentTerm(leaderTerm);

                            // Incrementally update the log
                            for(int i = this.server.getStateMachine().getLastLogIndex(); i < log.size(); i++) {
                                this.server.getStateMachine().getLog().add(log.get(i));
                                SysLog.logger.info("Appended new entry to old server at index " + i + " for term " +
                                        log.get(i).getTerm());
                            }

                            ret.setValue(this.server.getStateMachine().getCurrentTerm());
                            ret.setCondition(true);
                        }
                    }
                    // Terms match up, compare logs for enough consistency to append
                    else {
                        // Prev values match, append entry and advance state machine
                        if(this.server.getStateMachine().getLog().get(prevLogIndex).getTerm() == prevLogTerm) {
                            SysLog.logger.info("Received valid append entry request, adding to log");
                            LogEntry entry = log.get(prevLogIndex + 1);
                            this.server.getStateMachine().getLog().add(entry);
                            this.server.getStateMachine().setPrevLogIndex(prevLogIndex);
                            this.server.getStateMachine().setPrevLogTerm(prevLogTerm);
                            this.server.getStateMachine().setLastLogIndex(entry.getIndex());
                            this.server.getStateMachine().setLastLogTerm(entry.getTerm());
                            this.server.getStateMachine().getNextIndexes().set(leaderId - 1, entry.getIndex() + 1);
                            this.server.getStateMachine().getNextIndexes().set(this.server.getServerId() - 1,
                                    this.server.getStateMachine().getLastLogIndex() + 1);
                            ret.setServerId(this.server.getServerId());
                            ret.setNextIndex(this.server.getStateMachine().getLastLogIndex() + 1);
                            ret.setValue(this.server.getStateMachine().getCurrentTerm());
                            ret.setCondition(true);
                        }
                        // Unknown state
                        else
                        {
                            System.out.println(this.server.getStateMachine().getLog().get(prevLogIndex).getTerm());
                            System.out.println(prevLogTerm);
                        }
                    }
                }
                finally {
                    this.server.getStateMachine().getLogLock().unlock();
                    this.server.getStateMachine().getLastLogIndexLock().unlock();
                    this.server.getStateMachine().getLastLogTermLock().unlock();
                    this.server.getStateMachine().getPrevLogIndexLock().unlock();
                    this.server.getStateMachine().getPrevLogTermLock().unlock();
                }
            }

            // Signal if this server is in the FOLLOWER state
            if(this.server.getStateMachine().getCurrentState() == StateType.FOLLOWER &&
                    this.server.getStateMachine().getIsWaiting()) {
                SysLog.logger.fine("Signalling timeout condition in state machine");
                this.server.getStateMachine().getTimeoutLock().lock();
                try {
                    this.server.getStateMachine().getTimeoutCondition().signal();
                }
                finally {
                    this.server.getStateMachine().getTimeoutLock().unlock();
                }
            }

            // Update this server to be a follower after a step-down occurred (avoid wrongful signaling)
            if(setToFollower) {
                this.server.getStateMachine().setCurrentState(StateType.FOLLOWER);
            }
        }
        finally {
            this.server.getStateMachine().getCurrentStateLock().unlock();
            this.server.getStateMachine().getCurrentTermLock().unlock();
            this.server.getStateMachine().getVotedForLock().unlock();
            this.server.getStateMachine().getCommitIndexLock().unlock();
            this.server.getStateMachine().getLeaderIdLock().unlock();
            this.server.getStateMachine().getIsWaitingLock().unlock();
            this.server.getStateMachine().getNextIndexesLock().unlock();
        }

        SysLog.logger.finest("Exiting method");
        return ret;
    }

    // Initialize the RMI with the server info
    public boolean setupConnection() throws RemoteException {
        SysLog.logger.finest("Entering method");

        Registry reg = LocateRegistry.createRegistry(SysFiles.getBaseServerPort() + this.server.getServerId());
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
                if(this.log == null) {
                    SysLog.logger.fine("Sending APPENDENTRIES RPC to remote server");
                }
                else {
                    SysLog.logger.info("Sending APPENDENTRIES RPC to remote server");
                }
                ret = this.remoteServer.appendEntriesRPC(this.leaderId, this.leaderTerm, this.prevLogIndex, this.prevLogTerm,
                        this.log, this.commitIndex, this.nextIndexes);
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
