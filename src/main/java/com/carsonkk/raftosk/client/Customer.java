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

public class Customer extends Client {

    public Customer() {
        super();
    }

    public boolean processCommandRequest(String commandInput, Command command) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if(super.processCommandRequest(commandInput, command)) {
            return false;
        }
        else {
            switch (commandInput) {
                case "buy": {
                    command.setCommandType(CommandType.BUY);
                    System.out.println("How many tickets would you like to buy?");
                    System.out.println();

                    try {
                        command.setTicketAmount(Integer.parseInt(reader.readLine()));
                    } catch (NumberFormatException e) {
                        System.out.println("[ERR] Invalid user input: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }

                    System.out.println();
                    System.out.println("Sending request to purchase " + command.getTicketAmount() + " tickets to the server for " +
                            "processing, please wait...");
                    System.out.println();
                    break;
                }
                default: {
                    invalidCommand = true;
                    return false;
                }
            }
        }

        return true;
    }
}
