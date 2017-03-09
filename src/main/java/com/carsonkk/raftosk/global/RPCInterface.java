package main.java.com.carsonkk.raftosk.global;

import java.rmi.*;

public interface RPCInterface extends Remote
{
    int add(int a, int b) throws RemoteException;
    int sub(int a, int b) throws RemoteException;
}
