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

    public void processCommandRequest(ClientType clientType, String commandInput, Command command) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        int tickets;

        switch(commandInput) {
            case "quit": {
                quitCommand = true;
                break;
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
                break;
            }
            case "buy": {
                if(clientType == ClientType.CUSTOMER) {
                    command.setCommandType(CommandType.BUY);
                    System.out.println("How many tickets would you like to buy?");
                    System.out.println();
                    System.out.print("> ");

                    try {
                        command.setTicketAmount(Integer.parseInt(reader.readLine()));
                    }
                    catch(NumberFormatException e) {
                        System.out.println("[ERR] Invalid user input: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }

                    System.out.println();
                    System.out.println("Sending request to purchase " + command.getTicketAmount() + " tickets to the server for " +
                            "processing, please wait...");
                    System.out.println();
                }
                else {
                    invalidCommand = true;
                }
            }
            case "show": {
                if(clientType == ClientType.ADMINISTRATOR) {
                    command.setCommandType(CommandType.SHOW);
                    System.out.println("Sending request to server to display metadata, please wait...");
                    System.out.println();
                }
                else {
                    invalidCommand = true;
                }
                break;
            }
            case "change": {
                if(clientType == ClientType.ADMINISTRATOR) {
                    command.setCommandType(CommandType.CHANGE);
                    System.out.println("Would you like to add new servers or delete existing ones?");
                    System.out.println();
                    System.out.print("> ");
                    input = reader.readLine();
                    input = input.toLowerCase();
                    System.out.println();

                    switch(input) {
                        case "add": {
                            System.out.println("How many new servers would you like to add?");
                            System.out.println();
                            System.out.print("> ");

                            try {
                                command.setServerAmount(Integer.parseInt(reader.readLine()));
                            }
                            catch(NumberFormatException e) {
                                System.out.println("[ERR] Invalid user input: " + e.getMessage());
                                e.printStackTrace();
                                return;
                            }
                            command.setChangeType(ChangeType.ADD);

                            System.out.println();
                            System.out.println("Sending request to add " + command.getServerAmount() + " servers to the system, " +
                                    "please wait...");
                            System.out.println();
                            break;
                        }
                        case "delete": {
                            System.out.println("How many servers would you like to delete?");
                            System.out.println();
                            System.out.print("> ");

                            try {
                                command.setServerAmount(Integer.parseInt(reader.readLine()));
                            }
                            catch(NumberFormatException e) {
                                System.out.println("[ERR] Invalid user input: " + e.getMessage());
                                e.printStackTrace();
                                return;
                            }
                            command.setChangeType(ChangeType.DELETE);

                            System.out.println();
                            System.out.println("Sending request to delete " + command.getServerAmount() + " servers from the system, " +
                                    "please wait...");
                            System.out.println();
                            break;
                        }
                        default: {
                            invalidCommand = true;
                            break;
                        }
                    }
                }
                else {
                    invalidCommand = true;
                }
                break;
            }
            default: {
                invalidCommand = true;
                break;
            }
        }
    }
}
