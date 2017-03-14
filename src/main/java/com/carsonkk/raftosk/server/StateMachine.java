package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.floor;

// Keep track of server state, spawn worker threads as necessary
public class StateMachine {
    //region Private Members

    // State machine locks
    private final Lock currentStateLock = new ReentrantLock();
    private final Lock currentTermLock = new ReentrantLock();
    private final Lock votedForLock = new ReentrantLock();
    private final Lock leaderIdLock = new ReentrantLock();
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
        this.prevLogIndex = 0;
        this.prevLogTerm = 0;
        this.commitIndex = 0;
        this.votedFor = -1;
        this.leaderId = -1;
        this.executorService = Executors.newCachedThreadPool();

        // Leave with a serverId of -1, creates initial log sync point
        log.add(new LogEntry(this.lastLogIndex, this.lastLogTerm, -1));
        // Setup nextIndexes list, use for appending entries
        for(int i = 0; i < ServerProperties.getMaxServerCount(); i++) {
            nextIndexes.add(this.lastLogIndex + 1);
        }

        SysLog.logger.finer("Created new state machine");
    }

    public StateMachine(Server server) {
        this();
        this.server = server;
        SysLog.logger.finer("Created new state machine with server " + this.server);
    }

    //endregion

    //region Getters/Setters

    public Lock getCurrentStateLock() {
        return this.currentStateLock;
    }

    public Lock getCurrentTermLock() {
        return this.currentTermLock;
    }

    public Lock getVotedForLock() {
        return this.votedForLock;
    }

    public Lock getLeaderIdLock() {
        return this.leaderIdLock;
    }

    public Lock getTimeoutlock() {
        return this.timeoutLock;
    }

    public Condition getTimeoutCondition() {
        return this.timeoutCondition;
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

    public int getLastLogTerm() {
        return this.lastLogTerm;
    }

    public int getPrevLogIndex() {
        return this.prevLogIndex;
    }

    public int getPrevLogTerm() {
        return this.prevLogTerm;
    }

    public int getCommitIndex() {
        return this.commitIndex;
    }

    public int getLeaderId() {
        return this.leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
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
        int majorityVotes = ((int)floor(ServerProperties.getMaxServerCount() / 2.0)) + 1;
        boolean heartbeatReceived;
        boolean logActivity;

        switch(this.currentState) {
            case FOLLOWER: {
                SysLog.logger.fine("Began execution in FOLLOWER state");

                heartbeatReceived = false;
                electionTimeout = TimeUnit.MILLISECONDS.toNanos(getRandomElectionTimeout());

                while (!heartbeatReceived) {
                    this.timeoutLock.lock();
                    try {
                        heartbeatReceived = timeoutCondition.await(electionTimeout, TimeUnit.NANOSECONDS);

                        this.currentStateLock.lock();
                        this.votedForLock.lock();
                        try {
                            if (!heartbeatReceived) {
                                SysLog.logger.info("Timeout occurred, switching state from FOLLOWER to CANDIDATE");
                                this.currentState = StateType.CANDIDATE;
                                break;
                            }
                            else if (heartbeatReceived && this.votedFor != -1) {
                                break;
                            }
                        }
                        finally {
                            this.votedForLock.unlock();
                            this.currentStateLock.unlock();
                        }
                    }
                    catch (InterruptedException e) {
                        SysLog.logger.warning("State machine was interrupted during execution: " + e.getMessage());
                        e.printStackTrace();
                    }
                    finally {
                        this.timeoutLock.unlock();
                    }
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
                    this.currentTerm++;
                    this.votedFor = this.server.getServerId();
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
                while (electionTimeout > 0 && votesReceived < majorityVotes) {
                    // Start keeping track of time
                    startTime = System.nanoTime();
                    try {
                        // Loop over return values looking for completed tasks
                        futureTaskIterator = futureTaskList.iterator();
                        while(futureTaskIterator.hasNext()) {
                            FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                            if (futureTask.isDone()) {
                                ret = futureTask.get();

                                SysLog.logger.info("Vote request completed for future task " + futureTask +
                                        " with a value of " + ret.getValue() + " and a condition of " +
                                        ret.getCondition());

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
                        e.printStackTrace();
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
                    if (this.currentState == StateType.CANDIDATE) {
                        if (votesReceived >= majorityVotes) {
                            SysLog.logger.info("Switching state from CANDIDATE to LEADER");
                            this.currentState = StateType.LEADER;
                            this.leaderId = this.server.getServerId();
                        }
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

                this.currentStateLock.lock();
                this.currentTermLock.lock();
                this.votedForLock.lock();
                try {
                    ret = null;
                    logActivity = false;
                    futureTaskList = new ArrayList<>();

                    // Determine whether to log activity based on rpc type
                    if(SysLog.level > 4) {
                        logActivity = true;
                    }
                    else {
                        for(int i = 0; i < this.nextIndexes.size(); i++) {
                            if(this.lastLogIndex >= this.nextIndexes.get(i)) {
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
                                if(logActivity) {
                                    SysLog.logger.info("Server " + i +
                                            " is currently offline, couldn't send append entries RPC");
                                }
                                continue;
                            }

                            // Submit callable and add future to list
                            callable = new HandleRPC(this.server, RPCType.APPENDENTRIES, remoteServer,
                                    this.server.getServerId(), this.currentTerm, 0, 0, null, 0);
                            FutureTask<ReturnValueRPC> futureTask = new FutureTask<>(callable);
                            futureTaskList.add(futureTask);
                            this.executorService.submit(futureTask);
                            if(logActivity) {
                                SysLog.logger.info("Added server " + i + " to the list of appended-entry servers");
                            }
                        }
                    }
                }
                finally {
                    this.currentStateLock.unlock();
                    this.currentTermLock.unlock();
                    this.votedForLock.unlock();
                }

                // Set a maximum election timeout and attempt to get at least a majority of votes
                electionTimeout = TimeUnit.MILLISECONDS.toNanos(ServerProperties.getHeartbeatFrequency());
                while (electionTimeout > 0 && futureTaskList.size() > 0) {
                    // Start keeping track of time
                    startTime = System.nanoTime();
                    try {
                        futureTaskIterator = futureTaskList.iterator();
                        while(futureTaskIterator.hasNext()) {
                            FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                            if (futureTask.isDone()) {
                                ret = futureTask.get();

                                if(logActivity) {
                                    SysLog.logger.info("Append entry completed for future task " + futureTask +
                                            " with a value of " + ret.getValue() + " and a condition of " +
                                            ret.getCondition());
                                }

                                this.currentTermLock.lock();
                                try {
                                    if (ret.getValue() > this.currentTerm) {
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
                                SysLog.logger.info("Discovered a server with a higher term, stepping down");
                                this.currentTerm = ret.getValue();
                                this.votedFor = -1;
                                SysLog.logger.info("Switching state from LEADER to FOLLOWER");
                                this.currentState = StateType.FOLLOWER;
                                this.leaderId = -1;
                                break;
                            }
                        }
                        finally {
                            this.currentStateLock.unlock();
                            this.currentTermLock.unlock();
                            this.votedForLock.unlock();
                        }
                    }
                    catch (InterruptedException | ExecutionException e) { }

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
