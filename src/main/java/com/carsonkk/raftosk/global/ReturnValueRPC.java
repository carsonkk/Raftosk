package main.java.com.carsonkk.raftosk.global;

import java.io.Serializable;
import java.rmi.RemoteException;

// Object returned by any RPC call, contains integer and boolean values for result handling
public class ReturnValueRPC implements Serializable {
    //region Private Members

    private int value;
    private boolean condition;

    //endregion

    //region Constructors

    public ReturnValueRPC() throws RemoteException {
        this.value = 0;
        this.condition = false;

        SysLog.logger.finer("Created a new return value RPC");
    }

    public ReturnValueRPC(int value, boolean condition) throws RemoteException {
        this.value = value;
        this.condition = condition;

        SysLog.logger.finer("Created a new return value RPC with value " + this.value + " and condition " + this.condition);
    }

    //endregion

    //region Getters/Setters

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean getCondition() {
        return this.condition;
    }

    public void setCondition(boolean condition) {
        this.condition = condition;
    }

    //endregion
}
