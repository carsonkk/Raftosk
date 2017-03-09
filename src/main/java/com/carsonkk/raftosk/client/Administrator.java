package main.java.com.carsonkk.raftosk.client;

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

    public int run() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;
        RPCInterface s;
        Command command = new Command();

        System.out.println("/''''''''''''''''''''''''''''\\");
        System.out.println("|     Welcome to Raftosk     |");
        System.out.println("\\............................/");
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

            processCommandRequest(ClientType.ADMINISTRATOR, input, command);

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
