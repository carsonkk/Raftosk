package main.java.com.carsonkk.raftosk.global;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Utility class for connecting to a remote server
public final class ConnectToServer {
    //region Public Methods

    // Performs server connection
    public static RPCInterface connect(String address, int port)
    {
        SysLog.logger.fine("Entering method");

        Registry registry;
        RPCInterface server;

        // Attempt to connect to the specified server
        SysLog.logger.info("Connecting to remote server(" + address + ":" + port + ")");
        try {
            registry = LocateRegistry.getRegistry(address, port);
            server = (RPCInterface)registry.lookup("RPCInterface");
        }
        catch (Exception e) {
            SysLog.logger.warning("Could not connect to the remote server: " + e.getMessage());
            e.printStackTrace();
            SysLog.logger.fine("Exiting method");
            return null;
        }
        SysLog.logger.info("Connected to the server");

        SysLog.logger.fine("Exiting method");
        return server;
    }

    //endregion
}
