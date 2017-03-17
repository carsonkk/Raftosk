package main.java.com.carsonkk.raftosk.global;

import java.io.*;
import java.util.Properties;

import static java.lang.Math.floor;

// Handles reading in and storing configuration information from the .properties file
public final class SysFiles {
    //region Private Members

    private static String propertiesFilename;
    private static int initialTicketPool;
    private static int maxServerCount;
    private static int minElectionTimeout;
    private static int maxElectionTimeout;
    private static int heartbeatFrequency;
    private static int baseServerPort;
    private static String baseServerAddress;

    //endregion

    //region Getters/Setters

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

    public static int getHeartbeatFrequency() {
        return heartbeatFrequency;
    }

    public static int getBaseServerPort() {
        return baseServerPort;
    }

    public static String getBaseServerAddress() {
        return baseServerAddress;
    }

    //endregion

    //region Public Methods

    // Populates each property field from the file data
    public static void readPropertiesFile(String filename) {
        Properties properties = new Properties();
        InputStream inputStream = null;

        try {
            // Load properties stream
            inputStream = new FileInputStream(filename);
            properties.load(inputStream);

            // Get property values
            initialTicketPool = Integer.parseInt(properties.getProperty("initialTicketPool"));
            maxServerCount = Integer.parseInt(properties.getProperty("maxServerCount"));
            minElectionTimeout = Integer.parseInt(properties.getProperty("minElectionTimeout"));
            maxElectionTimeout = Integer.parseInt(properties.getProperty("maxElectionTimeout"));
            heartbeatFrequency = Integer.parseInt(properties.getProperty("heartbeatFrequency"));
            baseServerPort = Integer.parseInt(properties.getProperty("baseServerPort"));
            baseServerAddress = properties.getProperty("baseServerAddress");
        }
        catch (IOException e) {
            System.out.println("An error occurred while opening/reading in the properties file values: " + e.getMessage());
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    propertiesFilename = filename;
                }
                catch (IOException e) {
                    System.out.println("An error occurred while closing the properties file stream: " + e.getMessage());
                }
            }
        }
    }

    // Write out the current properties values to the file
    public static void writePropertiesFile() {
        Properties properties = new Properties();
        OutputStream outputStream = null;

        try {
            // Set property values
            properties.setProperty("initialTicketPool", Integer.toString(initialTicketPool));
            properties.setProperty("maxServerCount", Integer.toString(maxServerCount));
            properties.setProperty("minElectionTimeout", Integer.toString(minElectionTimeout));
            properties.setProperty("maxElectionTimeout", Integer.toString(maxElectionTimeout));
            properties.setProperty("heartbeatFrequency", Integer.toString(heartbeatFrequency));
            properties.setProperty("baseServerPort", Integer.toString(baseServerPort));
            properties.setProperty("baseServerAddress", baseServerAddress);

            // Save properties stream
            outputStream = new FileOutputStream(propertiesFilename);
            properties.store(outputStream, null);
        }
        catch (IOException e) {
            System.out.println("An error occurred while opening/reading in the properties file values: " + e.getMessage());
        }
        finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                }
                catch (IOException e) {
                    System.out.println("An error occurred while closing the properties file stream: " + e.getMessage());
                }
            }
        }
    }

    public static int getMajorityVote() {
        return ((int)floor(maxServerCount / 2.0)) + 1;
    }

    //endregion
}