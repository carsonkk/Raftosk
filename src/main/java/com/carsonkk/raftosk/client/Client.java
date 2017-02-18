package main.java.com.carsonkk.raftosk.client;

import main.java.com.carsonkk.raftosk.server.ServerInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private int returnValue;

    public Client() {
        returnValue = 0;
    }

    public static int main() {
        Client client = new Client();

        try {
            Registry reg = LocateRegistry.getRegistry("127.0.0.1", 9001);
            ServerInterface s = (ServerInterface)reg.lookup("ServerInterface");

            System.out.println("5 + 12 = " + s.add(5, 12));
            System.out.println("7 - 1 = " + s.sub(7, 1));
        } catch (Exception e) {
            System.out.println("[ERR] An issue occurred while the client was invoking RPCs: " + e.getMessage());
            e.printStackTrace();
            client.returnValue = 1;
        }

        return client.returnValue;
    }
}
