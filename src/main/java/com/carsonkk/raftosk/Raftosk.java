package main.java.com.carsonkk.raftosk;

import com.beust.jcommander.*;
import main.java.com.carsonkk.raftosk.client.Administrator;
import main.java.com.carsonkk.raftosk.client.Customer;
import main.java.com.carsonkk.raftosk.global.*;
import main.java.com.carsonkk.raftosk.server.Server;

import java.io.IOException;
import java.rmi.RemoteException;

// Point-of-Entry, parse command line options and delegate between acting as a server or client
public class Raftosk {
    //region Private Members

    @Parameter(names = {"-admin", "-a"}, description = "Specify if operating as an administrator in client mode")
    private boolean clientType;

    @Parameter(names = {"-id", "-i"}, description = "Specify this server's id", validateWith = ServerIdValidation.class)
    private int serverId;

    @Parameter(names = {"-offline", "-o"}, description = "Specify if this server should be started as offline")
    private boolean offline;

    @Parameter(names = {"-logging", "-l"}, description = "Choose verbosity level (OFF(0) - ALL(8)) of logging",
            validateWith = LogLevelValidation.class)
    private int logLevel;

    private RaftoskType raftoskType;

    //endregion

    //region Constructors

    public Raftosk() {
        clientType = false;
        serverId = -1;
        offline = false;
        logLevel = 3;
        raftoskType = RaftoskType.NULL;
    }

    //endregion

    //region Public Methods

    public static void main(String[] args) throws RemoteException {
        Raftosk raftosk = new Raftosk();
        String usage = "    raftosk [-s serverId] [-a] [-l logLevel]";
        SysFiles.readPropertiesFile("config.properties");

        // Read in command line options
        try {
            new JCommander(raftosk, args);
        }
        catch(ParameterException e) {
            System.out.println("Invalid or missing arguments. Usage:");
            System.out.println(usage);
            return;
        }

        // Parse command line options, determine mode
        try {
            // Both options were specified, invalid
            if((raftosk.serverId != -1 || raftosk.offline) && raftosk.clientType) {
                System.out.println("Invalid or missing arguments. Usage:");
                System.out.println(usage);
            }
            // Operate in server mode
            else if(raftosk.serverId != -1) {
                raftosk.raftoskType = RaftoskType.SERVER;
                SysLog.setup(raftosk.logLevel, raftosk.raftoskType);
                SysLog.logger.config("Operating in server mode, creating server object");
                Server server = new Server(raftosk.serverId, raftosk.offline);
                SysLog.logger.info("Initializing server " + raftosk.serverId);
                if(server.initializeServer() == 1) {
                    SysLog.logger.finest("Exiting method");
                    System.exit(1);
                }
                server.waitForExitState();
            }
            // Operate in client mode
            else {
                // Operate as an Administrator
                if(raftosk.clientType) {
                    raftosk.raftoskType = RaftoskType.ADMINISTRATOR;
                    SysLog.setup(raftosk.logLevel, raftosk.raftoskType);
                    SysLog.logger.config("Operating in client mode as an Administrator, creating Administrator object");
                    Administrator administrator = new Administrator();
                    SysLog.logger.info("Running client handler");
                    if(administrator.handleClient() == 1) {
                        SysLog.logger.finest("Exiting method");
                        System.exit(1);
                    }
                }
                // Operate as a Customer
                else {
                    raftosk.raftoskType = RaftoskType.CUSTOMER;
                    SysLog.setup(raftosk.logLevel, raftosk.raftoskType);
                    SysLog.logger.config("Operating in client mode as a Customer, creating Customer object");
                    Customer customer = new Customer();
                    SysLog.logger.info("Running client handler");
                    if(customer.handleClient() == 1) {
                        SysLog.logger.finest("Exiting method");
                        System.exit(1);
                    }
                }
            }
        } catch (IOException e) {
            SysLog.logger.severe("An issue occurred while creating the server/client object: " + e.getMessage());
            e.printStackTrace();
            SysLog.logger.finest("Exiting method");
            System.exit(1);
        }

        SysLog.logger.finest("Exiting method");
        System.exit(0);
    }

    //endregion
}