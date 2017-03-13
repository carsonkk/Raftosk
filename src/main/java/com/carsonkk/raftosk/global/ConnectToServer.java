package main.java.com.carsonkk.raftosk.global;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Utility class for connecting to a remote server
public final class ConnectToServer {
    //region Public Methods

    // Performs server connection
    public static RPCInterface connect(String address, int port, boolean logActivity)
    {
        SysLog.logger.finest("Entering method");

        Registry registry;
        RPCInterface server = null;

        // Attempt to connect to the specified server
        if(logActivity) {
            SysLog.logger.info("Connecting to remote server(" + address + ":" + port + ")");
        }
        try {
            registry = LocateRegistry.getRegistry(address, port);
            server = (RPCInterface)registry.lookup("RPCInterface");
            if(logActivity) {
                SysLog.logger.info("Connected to the server " + (port - ServerProperties.getBaseServerPort()));
            }
        }
        catch (RemoteException | NotBoundException e) {
            //SysLog.logger.warning("Could not connect to the remote server: " + e.getMessage());
            //e.printStackTrace();
        }

        SysLog.logger.finest("Exiting method");
        return server;
    }

    //endregion
}
