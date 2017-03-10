package main.java.com.carsonkk.raftosk.server;

import main.java.com.carsonkk.raftosk.global.RPCInterface;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ConsensusModule extends UnicastRemoteObject implements RPCInterface {
    private Thread thread;
    private String threadName;


    public ConsensusModule() throws RemoteException {
        super();
        threadName = "ConsensusModuleThread";
        thread = new Thread(this, threadName);

        thread.start();
    }

    public void SubmitCommandRPC() throws RemoteException {

    }

    public void RequestVoteRPC(int id, int term, int lastLogIndex, int lastLogTerm) throws RemoteException {

    }

    public void AppendEntriesRPC(int id, int term, int prevLogIndex, int prevLogTerm, ArrayList<LogEntry> entries, int lastCommitIndex) throws RemoteException {

    }

    public int setupConnection(int port) throws RemoteException {
        try
        {
            Registry reg = LocateRegistry.createRegistry(port);
            reg.rebind("RPCInterface", this);
            System.out.println("Server ready...");
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while the server was initializing its RMI: " + e.getMessage());
            e.printStackTrace();
           return 1;
        }

        return 0;
    }

    @Override
    public void run() {

    }
}
