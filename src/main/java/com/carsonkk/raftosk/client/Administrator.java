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

public class Administrator extends Client {

    public Administrator() {
        super();
    }

    public boolean processCommandRequest(String commandInput, Command command) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;

        if(super.processCommandRequest(commandInput, command)) {
            return false;
        }
        else {
            switch(commandInput) {
                case "show": {
                    command.setCommandType(CommandType.SHOW);
                    System.out.println("Sending request to server to display metadata, please wait...");
                    System.out.println();
                    break;
                }
                case "change": {
                    command.setCommandType(CommandType.CHANGE);
                    System.out.println("Would you like to add new servers or delete existing ones?");
                    System.out.println();
                    input = reader.readLine();
                    input = input.toLowerCase();
                    System.out.println();

                    switch(input) {
                        case "add": {
                            System.out.println("How many new servers would you like to add?");
                            System.out.println();

                            try {
                                command.setServerAmount(Integer.parseInt(reader.readLine()));
                            }
                            catch(NumberFormatException e) {
                                System.out.println("[ERR] Invalid user input: " + e.getMessage());
                                e.printStackTrace();
                                return false;
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

                            try {
                                command.setServerAmount(Integer.parseInt(reader.readLine()));
                            } catch (NumberFormatException e) {
                                System.out.println("[ERR] Invalid user input: " + e.getMessage());
                                e.printStackTrace();
                                return false;
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
                            return false;
                        }
                    }
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
