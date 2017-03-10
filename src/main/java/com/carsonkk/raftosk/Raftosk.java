package main.java.com.carsonkk.raftosk;

import com.beust.jcommander.*;
import main.java.com.carsonkk.raftosk.client.Administrator;
import main.java.com.carsonkk.raftosk.client.Customer;
import main.java.com.carsonkk.raftosk.global.ServerProperties;
import main.java.com.carsonkk.raftosk.server.Server;
import main.java.com.carsonkk.raftosk.client.Client;
import java.rmi.*;

public class Raftosk {
    @Parameter(names = {"-admin", "-a"}, description = "If operating in client mode, act as an administrator")
    private boolean clientTypeParam;

    @Parameter(names = {"-server", "-s"}, description = "Specify this server's id", validateWith = ServerIdValidation.class)
    private int serverId;

    public Raftosk() {
        clientTypeParam = false;
        serverId = -1;
    }

    public static void main(String[] args) throws RemoteException {
        Raftosk raftosk = new Raftosk();
        String usage = "    javac raftosk [-s serverId] | [-a]";

        // Read in command line options
        try {
            new JCommander(raftosk, args);
        }
        catch(ParameterException e) {
            System.out.println("[ERR] Invalid or missing arguments. Usage:");
            System.out.println(usage);
        }

        // Read in the properties file values
        ServerProperties.readPropertiesFile("config.properties");

        // Parse command line options, determine mode
        try {
            // Both options were specified, invalid
            if(raftosk.serverId != -1 && raftosk.clientTypeParam) {
                System.out.println("[ERR] Invalid or missing arguments. Usage:");
                System.out.println(usage);
            }
            // Operate in server mode
            else if(raftosk.serverId != -1) {
                Server server = new Server(raftosk.serverId);
                server.initializeServer();
            }
            // Operate in client mode
            else {
                // Operate as an Administrator
                if(raftosk.clientTypeParam) {
                    Administrator administrator = new Administrator();
                    administrator.handleClient();
                }
                // Operate as a Customer
                else {
                    Customer customer = new Customer();
                    customer.handleClient();
                }
            }
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while creating a server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}