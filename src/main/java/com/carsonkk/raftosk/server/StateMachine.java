package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// Keep track of server state, spawn worker threads as necessary
public class StateMachine implements Runnable {
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
    private int votedFor;

    //endregion

    //region Constructors

    public StateMachine() {
        this.server = null;
        this.consensusModule = new ConsensusModule();
        this.onlineServerIds = new ArrayList<Integer>();
        this.log = new ArrayList<LogEntry>();
        this.currentState = StateType.NULL;
        this.currentTerm = 0;
        this.lastLogIndex = 0;
        this.lastLogTerm = 0;
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

    public Condition getTimeoutCondition() {
        return this.timeoutCondition;
    }

    //endregion

    //region Public Methods

    @Override
    public void run() {
        int electionTimeout;
        boolean heartbeatReceived = false;
        boolean electionReceived = false;
        ExecutorService executorService;
        List<Future<ReturnValueRPC>> returnList = new ArrayList<Future<ReturnValueRPC>>();
        Callable<ReturnValueRPC> callable;

        try {
            while(true) {
                synchronized (this.currentState) {
                    switch(this.currentState) {
                        case FOLLOWER: {
                            // If no heartbeat before timeout, become candidate, otherwise restart timeout
                            this.lock.lock();
                            electionTimeout = getRandomElectionTimeout();
                            try {
                                while(!heartbeatReceived /*&& something to check for empty appendentryrpc*/) {
                                    heartbeatReceived = timeoutCondition.await(electionTimeout, TimeUnit.MILLISECONDS);
                                }
                                if(!heartbeatReceived) {
                                    this.currentState = StateType.CANDIDATE;
                                }
                                heartbeatReceived = false;
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

                            // Send RequestVoteRPCs to all possible servers
                            executorService = Executors.newFixedThreadPool(ServerProperties.getMaxServerCount());
                            returnList = new ArrayList<Future<ReturnValueRPC>>();
                            callable = new HandleRPC();

                            for(int i = 1; i < ServerProperties.getMaxServerCount(); i++) {
                                if(i != this.server.getServerId()) {
                                    RPCInterface remoteServer = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                                            ServerProperties.getBaseServerPort() + i);
                                    if(remoteServer == null) {
                                        continue;
                                    }
                                    try {
                                        HandleRPC handleRequestVoteRPC = new HandleRPC(this.server, RPCType.REQUESTVOTE, remoteServer,
                                                this.server.getServerId(), this.currentTerm, this.lastLogIndex, this.lastLogTerm);
                                        handleRequestVoteRPC.getThread().start();
                                    }
                                    catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            electionTimeout = getRandomElectionTimeout();
                            try {
                                while(!electionReceived /*&& something to check for empty appendentryrpc*/) {
                                    electionReceived = timeoutCondition.await(electionTimeout, TimeUnit.MILLISECONDS);
                                }
                                if(!electionReceived) {
                                    this.currentTerm++;
                                }
                            }
                            finally {
                                this.lock.unlock();
                            }
                            break;
                        }
                        case LEADER: {
                            //

                            break;
                        }
                        default: {
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
    }

    public static int getRandomElectionTimeout() {
        return ThreadLocalRandom.current().nextInt(ServerProperties.getMinElectionTimeout(), ServerProperties.getMaxElectionTimeout() + 1);
    }

    //endregion
}
