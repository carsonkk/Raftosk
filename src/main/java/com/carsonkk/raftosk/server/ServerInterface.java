package main.java.com.carsonkk.raftosk.server;

import java.rmi.*;

public interface ServerInterface extends Remote
{
    int add(int a, int b) throws RemoteException;
    int sub(int a, int b) throws RemoteException;
}
