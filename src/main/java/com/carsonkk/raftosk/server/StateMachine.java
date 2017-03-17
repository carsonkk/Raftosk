package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Keep track of server state, spawn worker threads as necessary
public class StateMachine {
    //region Private Members

    // State machine locks
    private final ReentrantLock logLock = new ReentrantLock();
    private final ReentrantLock currentStateLock = new ReentrantLock();
    private final ReentrantLock currentTermLock = new ReentrantLock();
    private final ReentrantLock lastLogIndexLock = new ReentrantLock();
    private final ReentrantLock lastLogTermLock = new ReentrantLock();
    private final ReentrantLock prevLogIndexLock = new ReentrantLock();
    private final ReentrantLock prevLogTermLock = new ReentrantLock();
    private final ReentrantLock commitIndexLock = new ReentrantLock();
    private final ReentrantLock votedForLock = new ReentrantLock();
    private final ReentrantLock leaderIdLock = new ReentrantLock();
    private final ReentrantLock isWaitingLock = new ReentrantLock();
    private final ReentrantLock appendEntriesLock = new ReentrantLock();
    private final Lock timeoutLock = new ReentrantLock();
    private final Condition timeoutCondition = timeoutLock.newCondition();

    private Server server;
    private ConsensusModule consensusModule;
    private List<LogEntry> log;
    private List<Integer> nextIndexes;
    private StateType currentState;
    private int currentTerm;
    private int lastLogIndex;
    private int lastLogTerm;
    private int prevLogIndex;
    private int prevLogTerm;
    private int commitIndex;
    private int votedFor;
    private int leaderId;
    private boolean isWaiting;
    private ExecutorService executorService;

    //endregion

    //region Constructors

    public StateMachine() {
        this.server = null;
        this.consensusModule = new ConsensusModule();
        this.log = new ArrayList<>();
        this.nextIndexes = new ArrayList<>();
        this.currentState = StateType.NULL;
        this.currentTerm = 0;
        this.lastLogIndex = 0;
        this.lastLogTerm = 0;
        this.prevLogIndex = -1;
        this.prevLogTerm = -1;
        this.commitIndex = 0;
        this.votedFor = -1;
        this.leaderId = -1;
        this.isWaiting = false;
        this.executorService = Executors.newCachedThreadPool();

        // Leave with a serverId of -1, creates initial log sync point
        Command dummyCommand = new Command();
        dummyCommand.setUniqueId(new UUID(0, 0));
        log.add(new LogEntry(this.lastLogIndex, this.lastLogTerm, dummyCommand));

        // Setup nextIndexes list, use for appending entries
        for(int i = 0; i < ServerProperties.getMaxServerCount(); i++) {
            nextIndexes.add(this.lastLogIndex + 1);
        }

        SysLog.logger.finer("Created new state machine");
    }

    public StateMachine(Server server) {
        this();
        this.server = server;
    }

    //endregion

    //region Getters/Setters

    public ReentrantLock getLogLock() {
        return this.logLock;
    }

    public ReentrantLock getCurrentStateLock() {
        return this.currentStateLock;
    }

    public ReentrantLock getCurrentTermLock() {
        return this.currentTermLock;
    }

    public ReentrantLock getLastLogIndexLock() {
        return this.lastLogIndexLock;
    }

    public ReentrantLock getLastLogTermLock() {
        return this.lastLogTermLock;
    }

    public ReentrantLock getPrevLogIndexLock() {
        return this.prevLogIndexLock;
    }

    public ReentrantLock getPrevLogTermLock() {
        return this.prevLogTermLock;
    }

    public ReentrantLock getCommitIndexLock() {
        return this.commitIndexLock;
    }

    public ReentrantLock getVotedForLock() {
        return this.votedForLock;
    }

    public ReentrantLock getLeaderIdLock() {
        return this.leaderIdLock;
    }

    public ReentrantLock getIsWaitingLock(){
        return this.isWaitingLock;
    }

    public ReentrantLock getAppendEntriesLock() {
        return this.appendEntriesLock;
    }

    public Lock getTimeoutLock() {
        return this.timeoutLock;
    }

    public Condition getTimeoutCondition() {
        return this.timeoutCondition;
    }

    public List<LogEntry> getLog() {
        return this.log;
    }

    public void setLog(List<LogEntry> log) {
        this.log = log;
    }

    public StateType getCurrentState() {
        return this.currentState;
    }

    public void setCurrentState(StateType currentState) {
        this.currentState = currentState;
    }

    public int getCurrentTerm() {
        return this.currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public int getVotedFor() {
        return this.votedFor;
    }

    public void setVotedFor(int votedFor) {
        this.votedFor = votedFor;
    }

    public int getLastLogIndex() {
        return this.lastLogIndex;
    }

    public void setLastLogIndex(int lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public int getLastLogTerm() {
        return this.lastLogTerm;
    }

    public void setLastLogTerm(int lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    public int getPrevLogIndex() {
        return this.prevLogIndex;
    }

    public void setPrevLogIndex(int prevLogIndex) {
        this.prevLogIndex = prevLogIndex;
    }

    public int getPrevLogTerm() {
        return this.prevLogTerm;
    }

    public void setPrevLogTerm(int prevLogTerm) {
        this.prevLogTerm = prevLogTerm;
    }

    public int getCommitIndex() {
        return this.commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public int getLeaderId() {
        return this.leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }

    public boolean getIsWaiting() {
        return this.isWaiting;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    //endregion

    //region Public Methods

    public void runMachine() throws RemoteException {
        SysLog.logger.finest("Entering method");

        List<FutureTask<ReturnValueRPC>> futureTaskList;
        Iterator<FutureTask<ReturnValueRPC>> futureTaskIterator;
        Callable<ReturnValueRPC> callable;
        RPCInterface remoteServer;
        ReturnValueRPC ret;
        long electionTimeout;
        long startTime;
        long endTime;
        int votesReceived;
        boolean heartbeatReceived;
        boolean logActivity;

        switch(this.currentState) {
            case FOLLOWER: {
                SysLog.logger.fine("Began execution in FOLLOWER state");

                this.timeoutLock.lock();
                try {
                    this.isWaitingLock.lock();
                    try {
                        this.isWaiting = true;
                    }
                    finally {
                        this.isWaitingLock.unlock();
                    }

                    electionTimeout = TimeUnit.MILLISECONDS.toNanos(getRandomElectionTimeout());
                    heartbeatReceived = timeoutCondition.await(electionTimeout, TimeUnit.NANOSECONDS);
                    this.isWaitingLock.lock();
                    try {
                        this.isWaiting = false;
                    }
                    finally {
                        this.isWaitingLock.unlock();
                    }
                    this.currentStateLock.lock();
                    this.votedForLock.lock();
                    try {
                        if (!heartbeatReceived) {
                            SysLog.logger.info("Timeout occurred, switching state from FOLLOWER to CANDIDATE");
                            this.currentState = StateType.CANDIDATE;
                            this.votedFor = this.server.getServerId();
                        }
                        else {
                            SysLog.logger.fine("Vote request/heartbeat received");
                        }
                    }
                    finally {
                        this.currentStateLock.unlock();
                        this.votedForLock.unlock();
                    }
                }
                catch (InterruptedException e) {
                    SysLog.logger.warning("State machine was interrupted during execution: " + e.getMessage());
                    //e.printStackTrace();
                }
                finally {
                    this.timeoutLock.unlock();
                }

                SysLog.logger.fine("Finished execution in FOLLOWER state");
                break;
            }
            case CANDIDATE: {
                SysLog.logger.fine("Began execution in CANDIDATE state");

                this.currentStateLock.lock();
                this.currentTermLock.lock();
                this.votedForLock.lock();
                try {
                    // Make sure the server is still a candidate
                    if(this.currentState != StateType.CANDIDATE) {
                        SysLog.logger.fine("Execution in CANDIDATE state was safely aborted");
                        break;
                    }

                    this.currentTerm++;
                    ret = null;
                    votesReceived = 1;
                    futureTaskList = new ArrayList<>();

                    // Send RequestVoteRPCs to all possible servers
                    for (int i = 1; i < ServerProperties.getMaxServerCount() + 1; i++) {
                        // Don't send vote for this server
                        if (i != this.server.getServerId()) {
                            // Connect to the server
                            remoteServer = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                    ServerProperties.getBaseServerPort() + i, true);
                            if (remoteServer == null) {
                                SysLog.logger.info("Server " + i +
                                        " is currently offline, couldn't send vote request RPC");
                                continue;
                            }

                            // Submit callable and add future to list
                            callable = new HandleRPC(this.server, RPCType.REQUESTVOTE, remoteServer,
                                    this.server.getServerId(), this.currentTerm, this.lastLogIndex,
                                    this.lastLogTerm);
                            FutureTask<ReturnValueRPC> futureTask = new FutureTask<>(callable);
                            futureTaskList.add(futureTask);
                            this.executorService.submit(futureTask);
                            SysLog.logger.info("Added server " + i + " to the list of vote-requested servers");
                        }
                    }
                }
                finally {
                    this.currentStateLock.unlock();
                    this.currentTermLock.unlock();
                    this.votedForLock.unlock();
                }

                // Set a maximum election timeout and attempt to get at least a majority of votes
                electionTimeout = TimeUnit.MILLISECONDS.toNanos(ServerProperties.getMaxElectionTimeout());
                while (electionTimeout > 0 && votesReceived < ServerProperties.getMajorityVote()) {
                    // Start keeping track of time
                    startTime = System.nanoTime();
                    try {
                        // Loop over return values looking for completed tasks
                        futureTaskIterator = futureTaskList.iterator();
                        while(futureTaskIterator.hasNext()) {
                            FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                            if (futureTask.isDone()) {
                                ret = futureTask.get();
                                SysLog.logger.info("Vote request completed with a value of " + ret.getValue() +
                                        " and a condition of " + ret.getCondition());

                                this.currentTermLock.lock();
                                try {
                                    if (ret.getCondition()) {
                                        votesReceived++;
                                    } else if (ret.getValue() > this.currentTerm) {
                                        break;
                                    }
                                    ret = null;
                                    futureTaskIterator.remove();
                                }
                                finally {
                                    this.currentTermLock.unlock();
                                }
                            }
                        }

                        // Handle stepping down if an existing candidate or leader was found
                        this.currentStateLock.lock();
                        this.currentTermLock.lock();
                        this.votedForLock.lock();
                        try {
                            if (ret != null && ret.getValue() > this.currentTerm) {
                                SysLog.logger.info("Discovered a higher term while requesting votes, stepping down");
                                this.currentTerm = ret.getValue();
                                this.votedFor = -1;
                                SysLog.logger.info("Switching state from CANDIDATE to FOLLOWER");
                                this.currentState = StateType.FOLLOWER;
                                break;
                            }
                        }
                        finally {
                            this.currentStateLock.unlock();
                            this.currentTermLock.unlock();
                            this.votedForLock.unlock();
                        }
                    }
                    catch (InterruptedException | ExecutionException e) {
                        SysLog.logger.warning("State machine was interrupted during execution: " + e.getMessage());
                        //e.printStackTrace();
                    }

                    // Finish keeping track of time, update electionTimeout
                    endTime = System.nanoTime();
                    electionTimeout -= (endTime - startTime);
                }

                // Cancel any remaining calls
                futureTaskIterator = futureTaskList.iterator();
                while(futureTaskIterator.hasNext()) {
                    FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                    futureTask.cancel(false);
                }

                // Only check if still a candidate
                this.currentStateLock.lock();
                try {
                    if (this.currentState == StateType.CANDIDATE && votesReceived >= ServerProperties.getMajorityVote()) {
                        SysLog.logger.info("Switching state from CANDIDATE to LEADER");
                        this.currentState = StateType.LEADER;
                        this.leaderId = this.server.getServerId();
                    }
                }
                finally {
                    this.currentStateLock.unlock();
                }

                SysLog.logger.fine("Finished execution in CANDIDATE state");
                break;
            }
            case LEADER: {
                SysLog.logger.fine("Began execution in LEADER state");

                this.appendEntriesLock.lock();
                try {
                    this.currentStateLock.lock();
                    this.currentTermLock.lock();
                    this.votedForLock.lock();
                    try {
                        // Make sure the server is still a leader
                        if (this.currentState != StateType.LEADER) {
                            SysLog.logger.fine("Execution in LEADER state was safely aborted");
                            break;
                        }

                        ret = null;
                        logActivity = false;
                        futureTaskList = new ArrayList<>();

                        // Determine whether to log activity based on rpc type
                        if (SysLog.level > 4) {
                            logActivity = true;
                        } else {
                            for (int i = 0; i < this.nextIndexes.size(); i++) {
                                if (this.lastLogIndex >= this.nextIndexes.get(i)) {
                                    logActivity = true;
                                    break;
                                }
                            }
                        }

                        // Send AppendEntriesRPCs to all possible servers
                        for (int i = 1; i < ServerProperties.getMaxServerCount() + 1; i++) {
                            // Don't send vote for this server
                            if (i != this.server.getServerId()) {
                                // Connect to the server
                                remoteServer = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                        ServerProperties.getBaseServerPort() + i, logActivity);
                                if (remoteServer == null) {
                                    if (logActivity) {
                                        SysLog.logger.info("Server " + i +
                                                " is currently offline, couldn't send append entries RPC");
                                    }
                                    continue;
                                }

                                // Submit callable and add future to list
                                callable = new HandleRPC(this.server, RPCType.APPENDENTRIES, remoteServer,
                                        this.server.getServerId(), this.currentTerm, this.prevLogIndex, this.prevLogTerm,
                                        null, this.commitIndex);
                                FutureTask<ReturnValueRPC> futureTask = new FutureTask<>(callable);
                                futureTaskList.add(futureTask);
                                this.executorService.submit(futureTask);
                                if (logActivity) {
                                    SysLog.logger.info("Added server " + i + " to the list of appended-entry servers");
                                }
                            }
                        }
                    } finally {
                        this.currentStateLock.unlock();
                        this.currentTermLock.unlock();
                        this.votedForLock.unlock();
                    }

                    // Set a maximum election timeout and attempt to get at least a majority of votes
                    electionTimeout = TimeUnit.MILLISECONDS.toNanos(ServerProperties.getHeartbeatFrequency());
                    while (electionTimeout > 0) {
                        // Start keeping track of time
                        startTime = System.nanoTime();
                        try {
                            futureTaskIterator = futureTaskList.iterator();
                            if (futureTaskIterator.hasNext()) {
                                while (futureTaskIterator.hasNext()) {
                                    FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                                    if (futureTask.isDone()) {
                                        ret = futureTask.get();
                                        if (logActivity) {
                                            SysLog.logger.fine("Heartbeat completed with a value of " + ret.getValue() +
                                                    " and a condition of " + ret.getCondition());
                                        }

                                        this.currentTermLock.lock();
                                        try {
                                            if (ret.getCondition() == false || ret.getValue() > this.currentTerm) {
                                                break;
                                            }
                                            ret = null;
                                            futureTaskIterator.remove();
                                        } finally {
                                            this.currentTermLock.unlock();
                                        }
                                    }
                                }

                                // Handle stepping down if an existing candidate or leader was found
                                this.currentStateLock.lock();
                                this.currentTermLock.lock();
                                this.votedForLock.lock();
                                try {
                                    if (ret != null && ret.getValue() > this.currentTerm) {
                                        SysLog.logger.info("Discovered a server with a higher term, stepping down");
                                        this.currentTerm = ret.getValue();
                                        this.votedFor = -1;
                                        SysLog.logger.info("Switching state from LEADER to FOLLOWER");
                                        this.currentState = StateType.FOLLOWER;
                                        this.leaderId = -1;
                                        break;
                                    }
                                } finally {
                                    this.currentStateLock.unlock();
                                    this.currentTermLock.unlock();
                                    this.votedForLock.unlock();
                                }
                            }
                        } catch (InterruptedException | ExecutionException e) {
                        }

                        // Finish keeping track of time, update electionTimeout
                        endTime = System.nanoTime();
                        electionTimeout -= (endTime - startTime);
                    }

                    // Cancel any remaining calls
                    futureTaskIterator = futureTaskList.iterator();
                    while (futureTaskIterator.hasNext()) {
                        futureTaskIterator.next().cancel(false);
                    }
                } finally {
                    this.appendEntriesLock.unlock();
                }

                SysLog.logger.fine("Finished execution in LEADER state");
                break;
            }
            default: {
                SysLog.logger.severe("The state machine entered in an invalid state: " + this.currentState);
                break;
            }
        }

        SysLog.logger.finest("Exiting method");
    }

    public static int getRandomElectionTimeout() {
        return ThreadLocalRandom.current().nextInt(ServerProperties.getMinElectionTimeout(),
                ServerProperties.getMaxElectionTimeout() + 1);
    }

    //endregion
}
