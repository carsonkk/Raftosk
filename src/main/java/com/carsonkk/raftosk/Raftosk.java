package main.java.com.carsonkk.raftosk;

import com.beust.jcommander.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Raftosk {

    @Parameter(names = {"-datacenter", "-d"}, description = "Operate in Datacenter mode")
    private boolean dcm = false;

    public static void main(String[] args) {
        Raftosk raftosk = new Raftosk();

        new JCommander(raftosk, args);

        raftosk.parseCommandLine();
        raftosk.readConfigFile();
    }

    public void parseCommandLine() {
        System.out.println("the flag is " + dcm);
    }

    public void readConfigFile() {
        Properties properties = new Properties();
        InputStream inputStream = null;

        try {

            inputStream = new FileInputStream("config.properties");

            // load a properties file
            properties.load(inputStream);

            // get the property value and print it out
            System.out.println(properties.getProperty("messageDelay"));
            System.out.println(properties.getProperty("initialTicketPool"));
            System.out.println(properties.getProperty("clientListenPort"));
            System.out.println(properties.getProperty("baseDatacenterListenPort"));
            System.out.println(properties.getProperty("baseDatacenterAddress"));
            System.out.println(properties.getProperty("maxDatatcenterCount"));

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
