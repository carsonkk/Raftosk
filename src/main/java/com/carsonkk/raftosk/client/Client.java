package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.global.ChangeType;
import main.java.com.carsonkk.raftosk.global.Command;
import main.java.com.carsonkk.raftosk.global.CommandType;
import main.java.com.carsonkk.raftosk.global.RPCInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    protected boolean quitCommand;
    protected boolean invalidCommand;

    public Client() {
        quitCommand = false;
        invalidCommand = false;
    }

    public int run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        RPCInterface s;
        Command command = new Command();

        System.out.println(" /''''''''''''''''''''''''''''\\");
        System.out.println("|      Welcome to Raftosk      |");
        System.out.println(" \\............................/");
        System.out.println();
        System.out.println();

        s = connect("127.0.0.1", 9001, "RPCInterface");
        if(s == null) {
            return 1;
        }

        //Handle client-server interaction
        while (true) {
            // Read in what command to send to the server
            System.out.println("Please enter a command to send to the server");
            System.out.println("(for help with commands, type \"help\")");
            System.out.println();
            input = reader.readLine();
            input = input.toLowerCase();
            System.out.println();

            if(!processCommandRequest(input, command)) {
                // Quit the run if "quit" was received
                if(quitCommand) {
                    System.out.println("Closing connection with the server and quitting...");
                    return 0;
                }

                // Restart loop if a valid command was not given
                if(invalidCommand) {
                    System.out.println("An invalid command was entered, please try again");
                    invalidCommand = false;
                    continue;
                }
            }
        }
    }

    public RPCInterface connect(String address, int port, String name)
    {
        Registry reg;
        RPCInterface s;

        //Attempt to connect to the specified server
        try {
            reg = LocateRegistry.getRegistry(address, port);
            s = (RPCInterface)reg.lookup(name);
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while the client was connecting to the server: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return s;
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
