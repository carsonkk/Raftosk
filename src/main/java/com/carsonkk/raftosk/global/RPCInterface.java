package main.java.com.carsonkk.raftosk.global;

import main.java.com.carsonkk.raftosk.server.LogEntry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RPCInterface extends Remote, Runnable {
    public void SubmitCommandRPC()throws RemoteException;
    public void RequestVoteRPC(int id, int term, int lastLogIndex, int lastLogTerm) throws RemoteException;
    public void AppendEntriesRPC(int id, int term, int prevLogIndex, int prevLogTerm, ArrayList<LogEntry> entries, int lastCommitIndex) throws RemoteException;
}
