package main.java.com.carsonkk.raftosk.global;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

// Validates that the given server ID was within the valid range of values
public class ServerIdValidation implements IParameterValidator {
    //region Public Methods

    // Validation handler
    public void validate(String name, String value) throws ParameterException{
        int serverId = Integer.parseInt(value);
        if(serverId < 1 || serverId > ServerProperties.getMaxServerCount()) {
            throw new ParameterException("Parameter " + name + " was outside the valid range");
        }
    }

    //endregion
}
