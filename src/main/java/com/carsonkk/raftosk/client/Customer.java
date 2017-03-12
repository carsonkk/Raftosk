package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.global.Command;
import main.java.com.carsonkk.raftosk.global.CommandType;
import main.java.com.carsonkk.raftosk.global.SysLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// Specific type of client, can buy tickets, cannot submit configuration changes or monitor server status
public class Customer extends Client {
    //region Constructors

    public Customer() {
        super();
        SysLog.logger.finer("Created new Customer");
    }

    //endregion

    //region Public Methods

    // Do specific request handling beyond wha the base client's handling performs
    public boolean processCommandRequest(String commandInput, Command command) throws IOException
    {
        SysLog.logger.fine("Entering method");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        if(super.processCommandRequest(commandInput, command)) {
            return false;
        }
        else {
            switch (commandInput) {
                case "BUY": {
                    SysLog.logger.info("Received BUY command");

                    command.setCommandType(CommandType.BUY);
                    System.out.println("How many tickets would you like to buy?");
                    System.out.println();

                    try {
                        command.setTicketAmount(Integer.parseInt(reader.readLine()));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid user input: " + e.getMessage());
                        SysLog.logger.warning("Invalid user input: " + e.getMessage());
                        SysLog.logger.fine("Exiting method");
                        return false;
                    }

                    System.out.println();
                    System.out.println("Sending request to purchase " + command.getTicketAmount() +
                            " tickets to the server for processing, please wait...");
                    System.out.println();
                    break;
                }
                default: {
                    SysLog.logger.info("Received Unknown command");

                    invalidCommand = true;
                    SysLog.logger.fine("Exiting method");
                    return false;
                }
            }
        }

        SysLog.logger.fine("Exiting method");
        return true;
    }

    //endregion
}
