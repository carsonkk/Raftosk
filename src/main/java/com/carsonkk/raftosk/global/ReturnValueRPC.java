package main.java.com.carsonkk.raftosk.global;

public class ReturnValueRPC {
    //region Private Members

    private int value;
    private boolean condition;

    //endregion

    //region Constructors

    public ReturnValueRPC() {
        this.value = 0;
        this.condition = false;
    }

    public ReturnValueRPC(int value, boolean condition) {
        this.value = value;
        this.condition = condition;
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
