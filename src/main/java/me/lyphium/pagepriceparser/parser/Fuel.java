package me.lyphium.pagepriceparser.parser;

public enum Fuel {

    DIESEL,
    BENZIN,
    AUTOGAS;

    public String getName() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    public int getId() {
        return ordinal();
    }

    public String toString() {
        return getName();
    }

    public static Fuel getByName(String name) {
        for (Fuel fuel : values()) {
            if (fuel.name().equalsIgnoreCase(name)) {
                return fuel;
            }
        }
        return null;
    }

    public static Fuel getById(int id) {
        return id >= 0 && id < values().length ? values()[id] : null;
    }

}