package main.java.com.carsonkk.raftosk.global;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ServerIdValidation implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException{
        int serverId = Integer.parseInt(value);
        if(serverId < 1 || serverId > ServerProperties.getMaxServerCount()) {
            throw new ParameterException("Parameter " + name + " was outside the valid range");
        }
    }
}
