package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.global.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
    public void handleClient() throws IOException {
        SysLog.logger.finest("Entering method");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        RPCInterface server;
        ReturnValueRPC ret = null;
        int serverId;
        Command command = new Command();

        // Initial heading
        System.out.println(" /''''''''''''''''''''''''''''\\");
        System.out.println("|      Welcome to Raftosk      |");
        System.out.println(" \\............................/");
        System.out.println();
        System.out.println();

        // Read in which server to connect to
        System.out.println("Please enter the id of the server you would like to connect to");
        System.out.println();
        try {
            serverId = Integer.parseInt(reader.readLine());
        }
        catch(NumberFormatException e) {
            System.out.println("Invalid user input: " + e.getMessage());
            SysLog.logger.warning("Invalid user input: " + e.getMessage());
            SysLog.logger.finest("Exiting method");
            return;
        }

        // Connect to the server
        server = ConnectToServer.connect(ServerProperties.getBaseServerAddress(), ServerProperties.getBaseServerPort() +
                serverId, true);
        if(server == null) {
            SysLog.logger.finest("Exiting method");
            return;
        }

        // Handle client-server interaction
        while (true) {
            // Read in what command to send to the server
            System.out.println("Please enter a command to send to the server");
            System.out.println("(for help with commands, type \"help\")");
            System.out.println();
            input = reader.readLine();
            input = input.toUpperCase();
            System.out.println();

            // Check for invalid commands or the quit command
            if(!processCommandRequest(input, command)) {
                // Quit the run if "quit" was received
                if(quitCommand) {
                    System.out.println("Closing connection with the server and quitting...");
                    SysLog.logger.finest("Exiting method");
                    return;
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

            // Submit command
            ret = server.submitCommandRPC(command);
            break;
        }

        if(ret == null) {
            System.out.println("An error occurred while processing your transaction, please try again");
        }
        else {
            if(ret.getCondition()) {
                System.out.println("Your request for " + command.getTicketAmount() +
                        " tickets has been successfully completed");
                System.out.println("(Only " + ret.getValue() + " tickets remain, come again soon!)");
            }
            else {
                if(ret.getValue() == -1) {
                    System.out.println(
                            "The server system is currently down or undergoing a leadership transition, please try again");
                }
                else {
                    System.out.println("Your request for " + command.getTicketAmount() +
                            " tickets could not be completed (there were only " + ret.getValue() +
                            " tickets left), please try again");
                }
            }
        }
        System.out.println();

        SysLog.logger.finest("Exiting method");
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

    //endregion
}
