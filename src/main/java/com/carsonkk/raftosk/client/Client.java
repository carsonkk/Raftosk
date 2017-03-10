package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.global.*;
import main.java.com.carsonkk.raftosk.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

// Top-level user interface for interacting with server system
public class Client {

    protected boolean quitCommand;
    protected boolean invalidCommand;

    public Client() {
        quitCommand = false;
        invalidCommand = false;
    }

    public void handleClient() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        RPCInterface server;
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
            System.out.println("[ERR] Invalid user input: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Connect to the server
        server = ConnectToServer.connect(ServerProperties.getBaseServerAddress(), ServerProperties.getBaseServerPort() + serverId);
        if(server == null) {
            return;
        }

        // Handle client-server interaction
        while (true) {
            // Read in what command to send to the server
            System.out.println("Please enter a command to send to the server");
            System.out.println("(for help with commands, type \"help\")");
            System.out.println();
            input = reader.readLine();
            input = input.toLowerCase();
            System.out.println();

            // Check for invalid commands or the quit command
            if(!processCommandRequest(input, command)) {
                // Quit the run if "quit" was received
                if(quitCommand) {
                    System.out.println("Closing connection with the server and quitting...");
                    return;
                }

                // Restart loop if a valid command was not given
                if(invalidCommand) {
                    System.out.println("An invalid command was entered, please try again");
                    invalidCommand = false;
                    continue;
                }
            }

            // Submit command
            server.submitCommandRPC(command);
            break;
        }
    }

    public boolean processCommandRequest(String commandInput, Command command) throws IOException {
        switch (commandInput) {
            case "quit": {
                quitCommand = true;
                return true;
            }
            case "help": {
                System.out.println("To select a command to send, type one of the following when prompted:");
                System.out.println("    - \"buy\": request to purchase tickets from the connected server (for Customers only)");
                System.out.println("    - \"show\": print out the current state of the state machine as well as the committed logs " +
                        "in the connected server (for Administrators only)");
                System.out.println("    - \"change\": change the configuration of the server system (for Administrators only)");
                System.out.println("    - \"help\": print out this information again");
                System.out.println("    - \"quit\": disconnect from the server and quit");
                System.out.println();
                return true;
            }
        }

        return false;
    }
}
