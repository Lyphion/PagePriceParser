package me.lyphium.pagepriceparser.parser;

import lombok.Getter;

public class PageParseThread extends Thread {

    @Getter
    private final long delay;

    public PageParseThread(long delay) {
        this.delay = delay;

        setName("PageParser");
        setDaemon(true);
    }

    @Override
    public synchronized void start() {

    }

    @Override
    public void run() {

    }

    public void cancel() {
        this.interrupt();
    }

}
