package main.java.com.carsonkk.raftosk;

import com.beust.jcommander.*;
import main.java.com.carsonkk.raftosk.server.Server;
import main.java.com.carsonkk.raftosk.client.Client;
import java.rmi.*;

public class Raftosk {
    @Parameter(names = {"-server", "-s"}, description = "Operate in Server mode")
    private boolean serverMode;

    private int returnValue;

    public Raftosk() {
        serverMode = false;
        returnValue = 0;
    }

    public static int main(String[] args) throws RemoteException {
        Raftosk raftosk = new Raftosk();
        new JCommander(raftosk, args);

        // Parse command line options, determine mode
        try {
            if(raftosk.serverMode == true) {
                Server server = new Server();
                raftosk.returnValue = server.main();
            } else {
                Client client = new Client();
                raftosk.returnValue = client.main();
            }
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while creating a server: " + e.getMessage());
            e.printStackTrace();
            raftosk.returnValue = 1;
        }

        return raftosk.returnValue;
    }
}
