package main.java.com.carsonkk.raftosk.global;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class ConnectToServer {
    public static RPCInterface connect(String address, int port)
    {
        Registry registry;
        RPCInterface server;

        // Attempt to connect to the specified server
        System.out.println("[LOG-1] Connecting to server at " + address + ":" + port);
        try {
            registry = LocateRegistry.getRegistry(address, port);
            server = (RPCInterface)registry.lookup("RPCInterface");
        }
        catch (Exception e) {
            System.out.println("[ERROR] An issue occurred while connecting to the server: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
        System.out.println("[LOG-1] Successfully connected to server");
        System.out.println();

        return server;
    }
}
