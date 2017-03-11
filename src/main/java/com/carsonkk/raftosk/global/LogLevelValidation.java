package main.java.com.carsonkk.raftosk.global;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class LogLevelValidation implements IParameterValidator {
    public void validate(String name, String value) throws ParameterException{
        int logLevel = Integer.parseInt(value);
        if(logLevel < 1 || logLevel > 8) {
            throw new ParameterException("Parameter " + name + " was outside the valid range");
        }
    }
}