package main.java.com.carsonkk.raftosk.global;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerProperties {
    private static int initialTicketPool;
    private static int maxServerCount;
    private static int minElectionTimeout;
    private static int maxElectionTimeout;
    private static int baseServerPort;
    private static String baseServerAddress;

    public static int getIntialTicketPool() {
        return initialTicketPool;
    }

    public static int getMaxServerCount() {
        return maxServerCount;
    }

    public static int getMinElectionTimeout() {
        return minElectionTimeout;
    }

    public static int getMaxElectionTimeout() {
        return maxElectionTimeout;
    }

    public static int getBaseServerPort() {
        return baseServerPort;
    }

    public static String getBaseServerAddress() {
        return baseServerAddress;
    }

    public static void readPropertiesFile(String filename) {
        Properties properties = new Properties();
        InputStream inputStream = null;

        try {
            // Load properties stream
            inputStream = new FileInputStream(filename);
            properties.load(inputStream);

            // Set property values
            initialTicketPool = Integer.parseInt(properties.getProperty("initialTicketPool"));
            maxServerCount = Integer.parseInt(properties.getProperty("maxServerCount"));
            minElectionTimeout = Integer.parseInt(properties.getProperty("minElectionTimeout"));
            maxElectionTimeout = Integer.parseInt(properties.getProperty("maxElectionTimeout"));
            baseServerPort = Integer.parseInt(properties.getProperty("baseServerPort"));
            baseServerAddress = properties.getProperty("baseServerAddress");
        }
        catch (IOException e) {
            System.out.println("[ERR] An error occurred while opening/reading in the properties file values: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    System.out.println("[ERR] An error occurred while closing the properties file stream: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
