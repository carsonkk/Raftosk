package main.java.com.carsonkk.raftosk.server;

import java.util.ArrayList;

public class StateMachine implements Runnable{
    private int currentTerm;
    private int votedFor;
    private int electionTimeout;
    private StateType currentState;
    private ArrayList<LogEntry> log;

    public StateMachine() {
        currentTerm = 1;
        votedFor = -1;
        electionTimeout = 5000;
        currentState = StateType.FOLLOWER;
        log = new ArrayList<LogEntry>();
    }

    public void setElectionTimeout(int value) {
        this.electionTimeout = value;
    }

    @Override
    public void run() {
        while(true) {
            switch(currentState) {
                case FOLLOWER: {
                    // If no heart beat before timeout, become candidate, otherwise restart timeout
                    try {
                        Thread.sleep(electionTimeout);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.currentState = StateType.CANDIDATE;
                    break;
                }
                case CANDIDATE: {
                    this.currentTerm++;

                    break;
                }
                case LEADER: {

                    break;
                }
                default: {

                    break;
                }
            }
        }
    }
}
