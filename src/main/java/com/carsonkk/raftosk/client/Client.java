package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.global.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

// Top-level user interface for interacting with server system
public class Client {
    //region Protected Members

    protected boolean quitCommand;
    protected boolean invalidCommand;

    //endregion

    //region Constructors

    public Client() {
        quitCommand = false;
        invalidCommand = false;

        SysLog.logger.finer("Created new Client");
    }

    //endregion

    //region Public Methods

    // Top-level handler for client interactions, delegates between Administrator and Customer options
    public int handleClient() throws IOException {
        SysLog.logger.finest("Entering method");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        RPCInterface server = null;
        ReturnValueRPC returnValueRPC = null;
        int serverId;
        int ret = 0;
        boolean commandResolved = false;
        Command command = new Command();

        // Initial heading
        System.out.println("''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''\\");
        System.out.println("|                     Welcome to Raftosk                     |");
        System.out.println("............................................................/");
        System.out.println();
        System.out.println();

        // Read in which server to connect to
        System.out.println("Please enter the id of the server you would like to connect to");
        System.out.println("(Select from 1 to " + ServerProperties.getMaxServerCount() +
                " for potentially available server IDs)");
        System.out.println();
        try {
            serverId = Integer.parseInt(reader.readLine().trim());
            System.out.println();
        }
        catch(NumberFormatException e) {
            System.out.println("Invalid user input: " + e.getMessage());
            SysLog.logger.warning("Invalid user input: " + e.getMessage());
            SysLog.logger.finest("Exiting method");
            return 1;
        }

        // Outside valid server ID range
        if(serverId < 1 || serverId > ServerProperties.getMaxServerCount()) {
            System.out.println("Invalid server ID selected, please try again");
            SysLog.logger.finest("Exiting method");
            return 1;
        }

        // Connect to the server
        while(server == null) {
            server = ConnectToServer.connect(ServerProperties.getBaseServerAddress(), ServerProperties.getBaseServerPort() +
                    serverId, true);
            if(server == null) {
                serverId = nextServerId(serverId);
                System.out.println("The chosen server was not available, trying next potential server...");
                System.out.println();
            }
        }
        System.out.println("Connected to Server " + serverId + "!");

        // Handle client-server interaction
        while (true) {
            // Read in what command to send to the server
            System.out.println("Please enter a command to send to the server");
            System.out.println("(for help with commands, type \"help\")");
            System.out.println();
            input = reader.readLine();
            input = input.toUpperCase().trim();
            System.out.println();

            // Check for invalid commands or the quit command
            if(!processCommandRequest(input, command)) {
                // Quit the run if "quit" was received
                if(quitCommand) {
                    System.out.println("Closing connection with the server and quitting...");
                    SysLog.logger.finest("Exiting method");
                    return 1;
                }
                // Restart loop if a valid command was not given
                else if(invalidCommand) {
                    System.out.println("An invalid command was entered, please try again");
                    invalidCommand = false;
                    continue;
                }
                else {
                    continue;
                }
            }

            // Set UUID and submit command
            command.setUniqueId(UUID.randomUUID());
            returnValueRPC = server.submitCommandRPC(command);
            break;
        }

        // Handle results
        while(!commandResolved) {
            switch(command.getCommandType()) {
                case BUY: {
                    if(returnValueRPC == null) {
                        System.out.println("A critical error occurred causing the client to crash while processing your " +
                                "transaction, please try again");
                        ret = 1;
                        commandResolved = true;
                    }
                    else {
                        if(returnValueRPC.getCondition()) {
                            System.out.println("Your request for " + command.getTicketAmount() +
                                    " tickets has been successfully completed");
                            System.out.println("(Only " + returnValueRPC.getValue() + " tickets left, come again soon!)");
                            commandResolved = true;
                        }
                        else {
                            if(returnValueRPC.getValue() == -1) {
                                System.out.println("The chosen server was either not the leader or not available, " +
                                        "retrying...");
                                System.out.println();
                            }
                            else {
                                System.out.println("Your request for " + command.getTicketAmount() +
                                        " tickets could not be completed (there were only " + returnValueRPC.getValue() +
                                        " tickets left), please try again");
                                commandResolved = true;
                            }
                        }
                    }
                    break;
                }
                case SHOW: {
                    if(returnValueRPC == null || !returnValueRPC.getCondition()) {
                        System.out.println("A critical error occurred causing the client to crash while processing your " +
                                "transaction, please try again");
                        ret = 1;
                    }
                    else {
                        System.out.println("''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''\\");
                        System.out.println("SERVER STATE:");
                        System.out.println("    " + returnValueRPC.getTicketPool() + " tickets are left");
                        System.out.println();
                        System.out.println("SERVER LOG:");
                        if(returnValueRPC.getLog().size() == 1) {
                            System.out.println("    (the log is currently empty)");
                        }
                        else {
                            for(int i = 1; i < returnValueRPC.getLog().size(); i++) {
                                System.out.println("    Entry " + i + ":");
                                System.out.println("        UUID:       " + returnValueRPC.getLog().get(i).getCommand().getUniqueId());
                                System.out.println("        Term:       " + returnValueRPC.getLog().get(i).getTerm());
                                System.out.println("        Command:    " + returnValueRPC.getLog().get(i).getCommand().getCommandType());
                                if(returnValueRPC.getLog().get(i).getCommand().getCommandType() == CommandType.BUY) {
                                    System.out.println("        Amount:     " + returnValueRPC.getLog().get(i).getCommand().getTicketAmount());
                                }
                                else if(returnValueRPC.getLog().get(i).getCommand().getCommandType() == CommandType.CHANGE) {
                                    System.out.println("        Type:       " + returnValueRPC.getLog().get(i).getCommand().getChangeType());
                                    System.out.println("        Amount:     " + returnValueRPC.getLog().get(i).getCommand().getServerAmount());
                                }
                                if(returnValueRPC.getCommitIndex() >= i) {
                                    System.out.println("        Committed?: Yes");
                                }
                                else {
                                    System.out.println("        Committed?: No");
                                }
                            }
                        }
                        System.out.println("............................................................/");
                    }
                    commandResolved = true;
                    break;
                }
            }

            if(!commandResolved) {
                server = null;
                while(server == null) {
                    serverId = nextServerId(serverId);
                    server = ConnectToServer.connect(ServerProperties.getBaseServerAddress(),
                            ServerProperties.getBaseServerPort() + serverId, true);
                }
                returnValueRPC = server.submitCommandRPC(command);
            }
        }
        System.out.println();

        SysLog.logger.finest("Exiting method");
        return ret;
    }

    // Process commands in the cases of quiting and helping
    public boolean processCommandRequest(String commandInput, Command command) throws IOException {
        SysLog.logger.finest("Entering method");

        switch (commandInput) {
            case "QUIT": {
                SysLog.logger.info("Received QUIT command");

                quitCommand = true;

                SysLog.logger.finest("Exiting method");
                return true;
            }
            case "HELP": {
                SysLog.logger.info("Received HELP command");

                System.out.println("To select a command to send, type one of the following when prompted:");
                System.out.println("    - \"buy\": request to purchase tickets from the connected server (for Customers " +
                        "only)");
                System.out.println("    - \"show\": print out the current state of the state machine as well as the " +
                        "committed logs in the connected server (for Administrators only)");
                System.out.println("    - \"change\": change the configuration of the server system (for Administrators " +
                        "only)");
                System.out.println("    - \"help\": print out this information again");
                System.out.println("    - \"quit\": disconnect from the server and quit");
                System.out.println();

                SysLog.logger.finest("Exiting method");
                return true;
            }
        }

        SysLog.logger.finest("Exiting method");
        return false;
    }

    // Cycle through server IDs
    public static int nextServerId(int serverId) {
        serverId++;
        if(serverId > ServerProperties.getMaxServerCount()) {
            serverId = 1;
        }
        return serverId;
    }

    //endregion
}
