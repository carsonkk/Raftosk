package main.java.com.carsonkk.raftosk.server;

import java.util.concurrent.ThreadLocalRandom;

public class Timeout implements Runnable {
    private Thread thread;
    private String threadName;
    private int minTimeout;
    private int maxTimeout;
    private int currentTimeout;

    public Timeout() {
        threadName = "TimeoutThread";
        thread = new Thread(this, threadName);
        this.minTimeout = 0;
        this.maxTimeout = Integer.MAX_VALUE;
        this.currentTimeout = 0;

        thread.start();
    }

    public Timeout(int minTimeout, int maxTimeout) {
        threadName = "TimeoutThread";
        thread = new Thread(this, threadName);
        this.minTimeout = minTimeout;
        this.maxTimeout = maxTimeout;
        this.setRandomCurrentTimeout();

        thread.start();
    }

    public void setMinTimeout(int minTimeout) {
        this.minTimeout = minTimeout;
    }

    public void setMaxTimeout(int maxTimeout) {
        this.maxTimeout = maxTimeout;
    }

    public void setCurrentTimeout(int currentTimeout) {
        this.currentTimeout = currentTimeout;
    }

    @Override
    public void run() {

    }

    public void setRandomCurrentTimeout() {
        this.currentTimeout = ThreadLocalRandom.current().nextInt(this.minTimeout, maxTimeout + 1);
    }
}
