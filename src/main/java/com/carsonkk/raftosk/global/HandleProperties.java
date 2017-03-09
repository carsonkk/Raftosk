package main.java.com.carsonkk.raftosk.global;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class HandleProperties {
    public static void readPropertiesFile(String filename) {
        Properties properties = new Properties();
        InputStream inputStream = null;

        try {

            inputStream = new FileInputStream(filename);

            // load a properties file
            properties.load(inputStream);

            // get the property value and print it out
            System.out.println(properties.getProperty("minimumElectionTimeout"));
            System.out.println(properties.getProperty("maximumElectionTimeout"));
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
