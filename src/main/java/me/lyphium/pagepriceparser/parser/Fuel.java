package me.lyphium.pagepriceparser.parser;

import lombok.Getter;

@Getter
public enum Fuel {

    DIESEL("Diesel", "Shell Diesel FuelSave", "Diesel"),
    LKW_DIESEL("LKW-Diesel", "Shell Truck Diesel", "LKW-Diesel"),
    SUPER_E10("Super E10", "Shell Super FuelSave E10", "Super E10"),
    SUPER_E5("Super E5", "Shell Super FuelSave E5", "Super E5"),
    SUPER_95("Super 95", "Shell Super FuelSave 95", null),
    SUPER_PLUS("SuperPlus", null, "SuperPlus"),
    AUTOGAS("Autogas", "Shell Autogas (LPG)", "Autogas");

    private final String name;
    private final String shellName, cleverName;

    Fuel(String name, String shellName, String cleverName) {
        this.name = name;
        this.shellName = shellName;
        this.cleverName = cleverName;
    }

    public int getId() {
        return ordinal();
    }

    public String toString() {
        return getName();
    }

    public static Fuel getById(int id) {
        return id >= 0 && id < values().length ? values()[id] : null;
    }

    public static Fuel getByName(String name) {
        for (Fuel fuel : values()) {
            if (name.equalsIgnoreCase(fuel.name()) || name.equalsIgnoreCase(fuel.getName())) {
                return fuel;
            } else if (name.equalsIgnoreCase(fuel.getShellName()) || name.equalsIgnoreCase(fuel.getCleverName())) {
                return fuel;
            }
        }
        return null;
    }

}