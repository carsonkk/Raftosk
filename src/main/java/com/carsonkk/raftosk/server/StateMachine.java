package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.floor;

// Keep track of server state, spawn worker threads as necessary
public class StateMachine implements Callable<Void> {
    //region Private Members

    private final Lock lock = new ReentrantLock();
    private final Condition timeoutCondition = lock.newCondition();

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
    }

    public StateMachine(Server server) {
        this();
        this.server = server;
        onlineServerIds.add(this.server.getServerId());
    }

    //endregion

    //region Getters/Setters

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

    @Override
    public Void call() throws RemoteException {
        ExecutorService executorService;
        List<Future<ReturnValueRPC>> returnList;
        Callable<ReturnValueRPC> callable;
        RPCInterface remoteServer;
        ReturnValueRPC ret = null;
        long electionTimeout;
        long startTime;
        long endTime;
        int votesReceived;
        int majorityVotes = ((int)floor(ServerProperties.getMaxServerCount() / 2.0)) + 1;
        boolean heartbeatReceived;

        try {
            while(true) {
                synchronized (this.currentState) {
                    switch(this.currentState) {
                        case FOLLOWER: {
                            // If no heartbeat before timeout, become candidate, otherwise restart timeout
                            this.lock.lock();
                            heartbeatReceived = false;
                            electionTimeout = getRandomElectionTimeout();
                            try {
                                while(!heartbeatReceived) {
                                    heartbeatReceived = timeoutCondition.await(electionTimeout, TimeUnit.MILLISECONDS);
                                }
                                if(!heartbeatReceived) {
                                    this.currentState = StateType.CANDIDATE;
                                }
                            }
                            finally {
                                this.lock.unlock();
                            }
                            break;
                        }
                        case CANDIDATE: {
                            this.lock.lock();
                            this.currentTerm++;
                            this.votedFor = this.server.getServerId();
                            votesReceived = 1;
                            executorService = Executors.newFixedThreadPool(ServerProperties.getMaxServerCount());
                            returnList = new ArrayList<>();

                            // Send RequestVoteRPCs to all possible servers
                            for(int i = 1; i < ServerProperties.getMaxServerCount(); i++) {
                                // Don't send vote for this server
                                if(i != this.server.getServerId()) {
                                    // Connect to the server
                                    remoteServer = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                            ServerProperties.getBaseServerPort() + i);
                                    if(remoteServer == null) {
                                        continue;
                                    }

                                    // Submit callable and add future to list
                                    callable = new HandleRPC(this.server, RPCType.REQUESTVOTE, remoteServer,
                                            this.server.getServerId(), this.currentTerm, this.lastLogIndex,
                                            this.lastLogTerm);
                                    Future<ReturnValueRPC> future = executorService.submit(callable);
                                    returnList.add(future);
                                }
                            }

                            // Set a maximum election timeout and attempt to get at least a majority of votes
                            electionTimeout = ServerProperties.getMaxElectionTimeout();
                            while(electionTimeout > 0 || votesReceived < majorityVotes) {
                                // Start keeping track of time
                                startTime = System.nanoTime();
                                // Loop over return values looking for completed tasks
                                for(Future<ReturnValueRPC> f : returnList) {
                                    try {
                                        if(f.isDone()) {
                                            ret = f.get();
                                            if(ret.getCondition()) {
                                                votesReceived++;
                                            }
                                            else if(ret.getValue() > this.currentTerm) {
                                                break;
                                            }
                                            ret = null;
                                            returnList.remove(f);
                                        }
                                    }
                                    catch (InterruptedException | ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                }
                                // Handle stepping down if an existing candidate or leader was found
                                if(ret != null && ret.getValue() > this.currentTerm) {
                                    this.currentTerm = ret.getValue();
                                    this.currentState = StateType.FOLLOWER;
                                    break;
                                }
                                // Finish keeping track of time, update electionTimeout
                                endTime = System.nanoTime();
                                electionTimeout -= ((endTime - startTime)/1000000);
                            }

                            // Only check if still a candidate
                            if(this.currentState == StateType.CANDIDATE) {
                                if(votesReceived >= majorityVotes) {
                                    this.currentState = StateType.LEADER;
                                }
                            }

                            executorService.shutdown();
                            this.lock.unlock();
                            break;
                        }
                        case LEADER: {
                            this.lock.lock();
                            executorService = Executors.newFixedThreadPool(ServerProperties.getMaxServerCount());
                            returnList = new ArrayList<>();

                            // Send RequestVoteRPCs to all possible servers
                            for(int i = 1; i < ServerProperties.getMaxServerCount(); i++) {
                                // Don't send vote for this server
                                if(i != this.server.getServerId()) {
                                    // Connect to the server
                                    remoteServer = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                            ServerProperties.getBaseServerPort() + i);
                                    if(remoteServer == null) {
                                        continue;
                                    }

                                    // Submit callable and add future to list
                                    callable = new HandleRPC(this.server, RPCType.APPENDENTRIES, remoteServer,
                                            this.server.getServerId(), this.currentTerm, 0, 0, null, 0);
                                    Future<ReturnValueRPC> future = executorService.submit(callable);
                                    returnList.add(future);
                                }
                            }

                            // Set a maximum election timeout and attempt to get at least a majority of votes
                            electionTimeout = ServerProperties.getMaxElectionTimeout();
                            while(electionTimeout > 0) {
                                // Start keeping track of time
                                startTime = System.nanoTime();
                                // Loop over return values looking for completed tasks
                                for(Future<ReturnValueRPC> f : returnList) {
                                    try {
                                        if(f.isDone()) {
                                            ret = f.get();
                                            if(ret.getValue() > this.currentTerm) {
                                                break;
                                            }
                                            ret = null;
                                            returnList.remove(f);
                                        }
                                    }
                                    catch (InterruptedException | ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                }
                                // Handle stepping down if an existing candidate or leader was found
                                if(ret != null && ret.getValue() > this.currentTerm) {
                                    this.currentTerm = ret.getValue();
                                    this.currentState = StateType.FOLLOWER;
                                    break;
                                }
                                // Finish keeping track of time, update electionTimeout
                                endTime = System.nanoTime();
                                electionTimeout -= ((endTime - startTime)/1000000);
                            }

                            Thread.sleep(ServerProperties.getHeartbeatFrequency());
                            this.lock.unlock();
                            break;
                        }
                        default: {
                            System.out.println("[ERROR] State machine entered in an invalid state");
                            break;
                        }
                    }
                }
            }
        }
        catch(InterruptedException e) {
            System.out.println("[ERR] The state machine thread was interrupted: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public static int getRandomElectionTimeout() {
        return ThreadLocalRandom.current().nextInt(ServerProperties.getMinElectionTimeout(),
                ServerProperties.getMaxElectionTimeout() + 1);
    }

    //endregion
}
