package main.java.com.carsonkk.raftosk.global;

import main.java.com.carsonkk.raftosk.server.LogEntry;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

// Defines data serialization interface used within RMI for client-to-server and server-to-server communication
public interface RPCInterface extends Remote {
    public ReturnValueRPC submitCommandRPC(Command command)throws RemoteException;
    public ReturnValueRPC requestVoteRPC(int id, int term, int lastLogIndex, int lastLogTerm) throws RemoteException;
    public ReturnValueRPC appendEntriesRPC(int id, int term, int prevLogIndex, int prevLogTerm, List<LogEntry> entries,
                                           int lastCommitIndex) throws RemoteException;
}
