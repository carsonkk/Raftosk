package main.java.com.carsonkk.raftosk;

import com.beust.jcommander.*;
import main.java.com.carsonkk.raftosk.client.Administrator;
import main.java.com.carsonkk.raftosk.client.Customer;
import main.java.com.carsonkk.raftosk.global.LogLevelValidation;
import main.java.com.carsonkk.raftosk.global.ServerIdValidation;
import main.java.com.carsonkk.raftosk.global.ServerProperties;
import main.java.com.carsonkk.raftosk.global.SysLog;
import main.java.com.carsonkk.raftosk.server.Server;

import java.rmi.*;
import java.util.logging.Level;

public class Raftosk {
    @Parameter(names = {"-admin", "-a"}, description = "If operating in client mode, act as an administrator")
    private boolean clientTypeParam;

    @Parameter(names = {"-server", "-s"}, description = "Specify this server's id", validateWith = ServerIdValidation.class)
    private int serverId;

    @Parameter(names = {"-logging", "-l"}, description = "Choose verbosity level (OFF(0) - ALL(8)) of logging", validateWith = LogLevelValidation.class)
    private int logLevel;

    public Raftosk() {
        clientTypeParam = false;
        serverId = -1;
        logLevel = 3;
    }

    public static void main(String[] args) throws RemoteException {
        Raftosk raftosk = new Raftosk();
        String usage = "    javac raftosk [-s serverId] | [-a]";
        ServerProperties.readPropertiesFile("config.properties");

        // Read in command line options
        try {
            new JCommander(raftosk, args);
        }
        catch(ParameterException e) {
            System.out.println("[ERR] Invalid or missing arguments. Usage:");
            System.out.println(usage);
        }

        // Init logger
        SysLog.setup(raftosk.logLevel);

        // Parse command line options, determine mode
        try {
            // Both options were specified, invalid
            if(raftosk.serverId != -1 && raftosk.clientTypeParam) {
                SysLog.logger.severe("Invalid or missing arguments. Usage:");
                SysLog.logger.severe(usage);

                System.out.println("[ERR] Invalid or missing arguments. Usage:");
                System.out.println(usage);
            }
            // Operate in server mode
            else if(raftosk.serverId != -1) {
                SysLog.logger.config("Operating in server mode, creating server object");
                Server server = new Server(raftosk.serverId);
                SysLog.logger.fine("Initializing server");
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