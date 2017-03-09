package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.global.Command;
import main.java.com.carsonkk.raftosk.global.CommandType;
import main.java.com.carsonkk.raftosk.global.RPCInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Customer extends Client {

    public Customer() {
        super();
    }

    public int run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        Registry reg = null;
        RPCInterface s = null;
        Command command = new Command();
        boolean quitCommand = false;
        boolean invalidCommand = false;

        System.out.println("/''''''''''''''''''''''''''''\\");
        System.out.println("|     WELCOME TO RAFTOSK     |");
        System.out.println("\\............................/");
        System.out.println();
        System.out.println();

        //Attempt to connect to the specified server
        try {
            reg = LocateRegistry.getRegistry("127.0.0.1", 9001);
            s = (RPCInterface)reg.lookup("RPCInterface");
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while the client was invoking RPCs: " + e.getMessage());
            e.printStackTrace();
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
            switch(input) {
                case "quit": {
                    quitCommand = true;
                    break;
                }
                case "help": {
                    System.out.println("TODO");
                    break;
                }
                case "show": {
                    command.setCommandType(CommandType.SHOW);
                    break;
                }
                case "change": {
                    command.setCommandType(CommandType.CHANGE);
                    break;
                }
                default: {
                    invalidCommand = true;
                    break;
                }
            }

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


            // A valid command was received, obtain any additional details needed

            System.out.println("d");
            break;
        }

        return 0;
    }
}
