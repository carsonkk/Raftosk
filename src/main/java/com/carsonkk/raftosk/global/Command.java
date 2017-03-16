package main.java.com.carsonkk.raftosk.global;

import java.io.Serializable;
import java.util.UUID;

// Metadata for a command that can be issued from a client to a server
public class Command implements Serializable {
    //region Private Members

    private CommandType commandType;
    private ChangeType changeType;
    private int ticketAmount;
    private int serverAmount;
    private UUID uniqueId;

    //endregion

    //region Constructors

    public Command() {
        this.commandType = CommandType.NULL;
        this.changeType = ChangeType.NULL;
        this.ticketAmount = -1;
        this.serverAmount = -1;
        this.uniqueId = null;

        SysLog.logger.finer("Created command");
    }

    //endregion

    //region Getters/Setters

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

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    //endregion
}
