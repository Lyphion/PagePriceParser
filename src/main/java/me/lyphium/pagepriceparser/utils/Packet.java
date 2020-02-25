package me.lyphium.pagepriceparser.utils;

import java.io.Serializable;

public abstract class Packet implements Serializable {

    private static final long serialVersionUID = 6246994186876393092L;

    private final boolean waiting;

    public Packet() {
        this(false);
    }

    public Packet(boolean waiting) {
        this.waiting = waiting;
    }

    public boolean shouldWait() {
        return waiting;
    }

}
