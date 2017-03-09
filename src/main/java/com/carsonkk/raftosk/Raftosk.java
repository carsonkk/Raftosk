package main.java.com.carsonkk.raftosk;

import com.beust.jcommander.*;
import main.java.com.carsonkk.raftosk.client.Administrator;
import main.java.com.carsonkk.raftosk.client.Customer;
import main.java.com.carsonkk.raftosk.server.Server;
import main.java.com.carsonkk.raftosk.client.Client;
import java.rmi.*;

public class Raftosk {
    @Parameter(names = {"-server", "-s"}, description = "Operate in server mode")
    private boolean serverModeParam;

    @Parameter(names = {"-admin", "-a"}, description = "If operating in client mode, act as an administrator")
    private boolean clientTypeParam;

    private int returnValue;

    public Raftosk() {
        serverModeParam = false;
        clientTypeParam = false;
        returnValue = 0;
    }

    public static int main(String[] args) throws RemoteException {
        Raftosk raftosk = new Raftosk();
        new JCommander(raftosk, args);

        // Parse command line options, determine mode
        try {
            if(raftosk.serverModeParam && raftosk.clientTypeParam) {
                System.out.println("[ERR] Invalid or missing arguments. Usage:");
                System.out.println("javac raftosk [-s][-a]");
            }
            else if(raftosk.serverModeParam) {
                Server server = new Server();
                raftosk.returnValue = server.main();
            }
            else {
                if(raftosk.clientTypeParam) {
                    Administrator administrator = new Administrator();
                    raftosk.returnValue = administrator.run();
                }
                else {
                    Customer customer = new Customer();
                    raftosk.returnValue = customer.run();
                }
            }
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while creating a server: " + e.getMessage());
            e.printStackTrace();
            raftosk.returnValue = 1;
        }

        return raftosk.returnValue;
    }
}
