package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.global.ChangeType;
import main.java.com.carsonkk.raftosk.global.Command;
import main.java.com.carsonkk.raftosk.global.CommandType;
import main.java.com.carsonkk.raftosk.global.SysLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// Specific type of client, can submit configuration changes as well as monitor server status, cannot buy tickets
public class Administrator extends Client {
    //region Constructors

    public Administrator() {
        super();

        SysLog.logger.finer("Created a new Administrator");
    }

    //endregion

    //region Public Methods

    // Do specific request handling beyond wha the base client's handling performs
    public boolean processCommandRequest(String commandInput, Command command) throws IOException
    {
        SysLog.logger.finest("Entering method");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;

        if(super.processCommandRequest(commandInput, command)) {
            SysLog.logger.finest("Exiting method");
            return false;
        }
        else {
            switch(commandInput) {
                case "SHOW": {
                    SysLog.logger.info("Received SHOW command");

                    command.setCommandType(CommandType.SHOW);

                    System.out.println("Sending request to server to display metadata, please wait...");
                    System.out.println();
                    break;
                }
                case "CHANGE": {
                    SysLog.logger.info("Received CHANGE command");

                    command.setCommandType(CommandType.CHANGE);
                    System.out.println("Would you like to add new servers or delete existing ones?");
                    System.out.println();
                    input = reader.readLine();
                    input = input.toUpperCase().trim();
                    System.out.println();

                    switch(input) {
                        case "ADD": {
                            SysLog.logger.info("Received ADD sub-command");

                            System.out.println("How many new servers would you like to add?");
                            System.out.println();

                            try {
                                command.setServerAmount(Integer.parseInt(reader.readLine().trim()));
                            }
                            catch(NumberFormatException e) {

                                System.out.println("Invalid user input: " + e.getMessage());
                                SysLog.logger.warning("Invalid user input: " + e.getMessage());
                                SysLog.logger.finest("Exiting method");
                                return false;
                            }
                            command.setChangeType(ChangeType.ADD);

                            System.out.println();
                            System.out.println("Sending request to add " + command.getServerAmount() +
                                    " servers to the system, please wait...");
                            System.out.println();
                            break;
                        }
                        case "DELETE": {
                            SysLog.logger.info("Received DELETE sub-command");

                            System.out.println("How many servers would you like to delete?");
                            System.out.println();

                            try {
                                command.setServerAmount(Integer.parseInt(reader.readLine().trim()));
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid user input: " + e.getMessage());
                                SysLog.logger.warning("Invalid user input: " + e.getMessage());
                                SysLog.logger.finest("Exiting method");
                                return false;
                            }
                            command.setChangeType(ChangeType.DELETE);

                            System.out.println();
                            System.out.println("Sending request to delete " + command.getServerAmount() +
                                    " servers from the system, please wait...");
                            System.out.println();
                            break;
                        }
                        default: {
                            SysLog.logger.info("Received Unknown sub-command");

                            invalidCommand = true;
                            SysLog.logger.finest("Exiting method");
                            return false;
                        }
                    }
                    break;
                }
                default: {
                    SysLog.logger.info("Received Unknown command");

                    invalidCommand = true;
                    SysLog.logger.finest("Exiting method");
                    return false;
                }
            }
        }

        SysLog.logger.finest("Exiting method");
        return true;
    }

    //endregion
}
