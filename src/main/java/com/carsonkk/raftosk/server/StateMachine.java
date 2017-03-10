package main.java.com.carsonkk.raftosk.server;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class StateMachine implements Runnable {
    private Thread thread;
    private String threadName;
    private int currentTerm;
    private int votedFor;
    private Timeout electionTimeout;
    private StateType currentState;
    private ArrayList<LogEntry> log;

    public StateMachine() {
        threadName = "StateMachineThread";
        thread = new Thread(this, threadName);
        currentTerm = 0;
        votedFor = -1;
        electionTimeout = new Timeout(3000, 5000);
        currentState = StateType.NULL;
        log = new ArrayList<LogEntry>();

        thread.start();
    }



    public void setCurrentState(StateType currentState) {
        this.currentState = currentState;
    }

    public StateType getCurrentState() {
        return this.currentState;
    }

    @Override
    public void run() {
        try {
            while(true) {
                synchronized (this.currentState) {
                    switch(this.currentState) {
                        case FOLLOWER: {
                            // If no heart beat before timeout, become candidate, otherwise restart timeout
                            synchronized (this.electionTimeout) {
                                while(this.electionTimeout.isWaiting()) {
                                    this.electionTimeout.wait();
                                }

                            }
                            Thread.sleep(ThreadLocalRandom.current().nextInt(minElectionTimeout, maxElectionTimeout + 1));
                            this.currentState = StateType.CANDIDATE;
                            break;
                        }
                        case CANDIDATE: {
                            //this.currentTerm++;
                            //send out election rpcs to each server
                            //start timeout thread
                            //if received notify of current leader/higher term step down
                            //if timed out, start new election
                            //if receive majority of votes, become leader
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
}
