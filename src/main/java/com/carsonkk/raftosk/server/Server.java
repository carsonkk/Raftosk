package main.java.com.carsonkk.raftosk.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server extends UnicastRemoteObject implements ServerInterface
{
    private int returnValue;

    public Server() throws RemoteException {
        super();

        returnValue = 0;
    }

    public int add(int a, int b) throws RemoteException {
        return a + b;
    }

    public int sub(int a, int b) throws RemoteException {
        return a - b;
    }

    public static int main() throws RemoteException {
        Server server = new Server();

        try
        {
            Registry reg = LocateRegistry.createRegistry(9001);
            Server dc = new Server();
            reg.rebind("ServerInterface", dc);
            System.out.println("Server ready...");
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while the server was initializing its RMI: " + e.getMessage());
            e.printStackTrace();
            server.returnValue = 1;
        }

        return server.returnValue;
    }
}