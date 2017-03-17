package main.java.com.carsonkk.raftosk.server;

// Define each state the state machine can be in
public enum StateType {
    NULL, OFFLINE, FOLLOWER, CANDIDATE, LEADER
}
