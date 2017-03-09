package main.java.com.carsonkk.raftosk.global;

public class Command {
    private CommandType commandType;
    private ChangeType changeType;
    private int ticketAmount;
    private int serverAmount;

    public Command() {
        commandType = CommandType.NULL;
        changeType = ChangeType.NULL;
        ticketAmount = -1;
        serverAmount = -1;
    }

    public CommandType getCommandType() {
        return this.commandType;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }

    public ChangeType getChangeType() {
        return this.changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public int getTicketAmount() {
        return this.ticketAmount;
    }

    public void setTicketAmount(int ticketAmount) {
        this.ticketAmount = ticketAmount;
    }

    public int getServerAmount() {
        return this.serverAmount;
    }

    public void setServerAmount(int serverAmount) {
        this.serverAmount = serverAmount;
    }
}
