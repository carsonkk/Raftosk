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
    private final Lock timeoutlock = new ReentrantLock();
    private final Condition timeoutCondition = timeoutlock.newCondition();

    private Server server;
    private ConsensusModule consensusModule;
    private List<Integer> onlineServerIds;
    private List<LogEntry> log;
    private StateType currentState;
    private int currentTerm;
    private int lastLogIndex;
    private int lastLogTerm;
    private int prevLogIndex;
    private int prevLogTerm;
    private int commitIndex;
    private int votedFor;

    //endregion

    //region Constructors

    public StateMachine() {
        this.server = null;
        this.consensusModule = new ConsensusModule();
        this.onlineServerIds = new ArrayList<>();
        this.log = new ArrayList<>();
        this.currentState = StateType.NULL;
        this.currentTerm = 0;
        this.lastLogIndex = 0;
        this.lastLogTerm = 0;
        this.prevLogIndex = 0;
        this.prevLogTerm = 0;
        this.commitIndex = 0;
        this.votedFor = -1;
        // Leave with a serverId of -1, creates initial log sync point
        log.add(new LogEntry(this.lastLogIndex, this.lastLogTerm, -1));
        SysLog.logger.finer("Created new state machine");
    }

    public StateMachine(Server server) {
        this();
        this.server = server;
        onlineServerIds.add(this.server.getServerId());
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

    public Lock getTimeoutlock() {
        return this.timeoutlock;
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

    public Condition getTimeoutCondition() {
        return this.timeoutCondition;
    }

    //endregion

    //region Public Methods

    public Void runMachine() throws RemoteException {
        SysLog.logger.fine("Entering method");

        ExecutorService executorService;
        List<FutureTask<ReturnValueRPC>> futureTaskList;
        Iterator<FutureTask<ReturnValueRPC>> futureTaskIterator;
        Callable<ReturnValueRPC> callable;
        RPCInterface remoteServer;
        ReturnValueRPC ret;
        double electionTimeout;
        long startTime;
        long endTime;
        int votesReceived;
        int majorityVotes = ((int)floor(ServerProperties.getMaxServerCount() / 2.0)) + 1;
        boolean heartbeatReceived;

        while(true) {
            switch(this.currentState) {
                case FOLLOWER: {
                    SysLog.logger.fine("Began execution in FOLLOWER state");

                    heartbeatReceived = false;
                    electionTimeout = (double) getRandomElectionTimeout();

                    while (!heartbeatReceived) {
                        this.timeoutlock.lock();
                        try {
                            heartbeatReceived = timeoutCondition.await((int) electionTimeout, TimeUnit.MILLISECONDS);

                            this.currentStateLock.lock();
                            this.votedForLock.lock();
                            try {
                                if (!heartbeatReceived) {
                                    SysLog.logger.info("Switching state from FOLLOWER to CANDIDATE");
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
                            e.printStackTrace();
                        }
                        finally {
                            this.timeoutlock.unlock();
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
                        executorService = Executors.newFixedThreadPool(ServerProperties.getMaxServerCount());
                        futureTaskList = new ArrayList<>();

                        // Send RequestVoteRPCs to all possible servers
                        for (int i = 1; i < ServerProperties.getMaxServerCount() + 1; i++) {
                            // Don't send vote for this server
                            if (i != this.server.getServerId()) {
                                // Connect to the server
                                remoteServer = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                        ServerProperties.getBaseServerPort() + i);
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
                                executorService.execute(futureTask);
                                SysLog.logger.info("Added server " + i + " to the list of vote requested servers");
                            }
                        }

                        // Set a maximum election timeout and attempt to get at least a majority of votes
                        electionTimeout = (double) ServerProperties.getMaxElectionTimeout();
                        while (electionTimeout > 0.0 && votesReceived < majorityVotes) {
                            try {
                                // Start keeping track of time
                                startTime = System.nanoTime();

                                // Loop over return values looking for completed tasks
                                futureTaskIterator = futureTaskList.iterator();
                                while(futureTaskIterator.hasNext()) {
                                    FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                                    if (futureTask.isDone()) {
                                        SysLog.logger.info("Vote request completed for future task " + futureTask);
                                        ret = futureTask.get();
                                        if (ret.getCondition()) {
                                            votesReceived++;
                                        } else if (ret.getValue() > this.currentTerm) {
                                            break;
                                        }
                                        ret = null;
                                        futureTaskIterator.remove();
                                    }
                                }

                                // Handle stepping down if an existing candidate or leader was found
                                if (ret != null && ret.getValue() > this.currentTerm) {
                                    SysLog.logger.info("Discovered a higher term while requesting votes,  stepping down");
                                    this.currentTerm = ret.getValue();
                                    SysLog.logger.info("Switching state from CANDIDATE to FOLLOWER");
                                    this.currentState = StateType.FOLLOWER;
                                    break;
                                }

                                // Finish keeping track of time, update electionTimeout
                                endTime = System.nanoTime();
                                electionTimeout -= ((endTime - startTime) / 1000000.0);
                            } catch (InterruptedException | ExecutionException e) {
                                //SysLog.logger.warning("State machine was interrupted during execution: " +
                                e.getMessage();
                                e.printStackTrace();
                            }
                        }

                        // Only check if still a candidate
                        if (this.currentState == StateType.CANDIDATE) {
                            if (votesReceived >= majorityVotes) {
                                SysLog.logger.info("Switching state from CANDIDATE to LEADER");
                                this.currentState = StateType.LEADER;
                            }
                        }

                        executorService.shutdown();
                    }
                    finally {
                        this.currentStateLock.unlock();
                        this.currentTermLock.unlock();
                        this.votedForLock.unlock();
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
                        executorService = Executors.newFixedThreadPool(ServerProperties.getMaxServerCount());
                        futureTaskList = new ArrayList<>();

                        // Send AppendEntriesRPCs to all possible servers
                        for (int i = 1; i < ServerProperties.getMaxServerCount() + 1; i++) {
                            // Don't send vote for this server
                            if (i != this.server.getServerId()) {
                                // Connect to the server
                                remoteServer = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                        ServerProperties.getBaseServerPort() + i);
                                if (remoteServer == null) {
                                    SysLog.logger.info("Server " + i +
                                            " is currently offline, couldn't send append entries RPC");
                                    continue;
                                }

                                // Submit callable and add future to list
                                callable = new HandleRPC(this.server, RPCType.APPENDENTRIES, remoteServer,
                                        this.server.getServerId(), this.currentTerm, 0, 0, null, 0);
                                FutureTask<ReturnValueRPC> futureTask = new FutureTask<>(callable);
                                futureTaskList.add(futureTask);
                                executorService.execute(futureTask);
                                SysLog.logger.info("Added server " + i + " to the list of appended entry servers");
                            }
                        }

                        // Set a maximum election timeout and attempt to get at least a majority of votes
                        electionTimeout = (double) ServerProperties.getHeartbeatFrequency();
                        while (electionTimeout > 0.0 && futureTaskList.size() > 0) {
                            try {
                                // Start keeping track of time
                                startTime = System.nanoTime();

                                futureTaskIterator = futureTaskList.iterator();
                                while(futureTaskIterator.hasNext()) {
                                    FutureTask<ReturnValueRPC> futureTask = futureTaskIterator.next();
                                    if (futureTask.isDone()) {
                                        ret = futureTask.get();
                                        SysLog.logger.info("Append entry completed for future task " + futureTask +
                                                " with a value of " + ret.getValue() + " and a condition of " +
                                                ret.getCondition());
                                        if (ret.getValue() > this.currentTerm) {
                                            break;
                                        }
                                        ret = null;
                                        futureTaskIterator.remove();
                                    }
                                }

//                            // Loop over return values looking for completed tasks
//                            for (FutureTask<ReturnValueRPC> f : futureTaskList) {
//                                try {
//                                    if (f.isDone()) {
//                                        SysLog.logger.info("Append entry completed");
//                                        ret = f.get();
//                                        if (ret.getValue() > this.currentTerm) {
//                                            break;
//                                        }
//                                        ret = null;
//                                        futureTaskList.remove(f);
//                                    }
//                                } catch (Exception e) {
//                                    SysLog.logger.warning("State machine was interrupted during execution: " +
//                                            e.getMessage());
//                                    //e.printStackTrace();
//                                }
//                            }
                                // Handle stepping down if an existing candidate or leader was found
                                if (ret != null && ret.getValue() > this.currentTerm) {
                                    SysLog.logger.info("Discovered a server with a higher term, stepping down");
                                    this.currentTerm = ret.getValue();
                                    SysLog.logger.info("Switching state from LEADER to FOLLOWER");
                                    this.currentState = StateType.FOLLOWER;
                                    break;
                                }

                                // Finish keeping track of time, update electionTimeout
                                endTime = System.nanoTime();
                                electionTimeout -= ((endTime - startTime) / 1000000.0);
                            }
                            catch (InterruptedException | ExecutionException e) {
                                //SysLog.logger.warning("State machine was interrupted during execution: " +
                                e.printStackTrace();
                            }
                        }

                        executorService.shutdown();
                    }
                    finally {
                        this.currentStateLock.unlock();
                        this.currentTermLock.unlock();
                        this.votedForLock.unlock();
                    }

                    SysLog.logger.fine("Finished execution in LEADER state");
                    break;
                }
                default: {
                    SysLog.logger.severe("The state machine entered in an invalid state: " + this.currentState);
                    break;
                }
            }
        }
    }

    public static int getRandomElectionTimeout() {
        return ThreadLocalRandom.current().nextInt(ServerProperties.getMinElectionTimeout(),
                ServerProperties.getMaxElectionTimeout() + 1);
    }

    //endregion
}
