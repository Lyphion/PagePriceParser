package me.lyphium.pagepriceparser.parser;

import lombok.Getter;

import java.awt.*;

@Getter
public enum Fuel {

    DIESEL(0, "Diesel", "#43e81a", "Shell Diesel FuelSave", "Diesel"),
    LKW_DIESEL(1, "LKW-Diesel", "#ba7303", "Shell Truck Diesel", "LKW-Diesel"),
    SUPER_E10(2, "Super E10", "#cbbe28", "Shell Super FuelSave E10", "Super E10"),
    SUPER_E5(3, "Super E5", "#651ab0", "Shell Super FuelSave E5", "Super E5"),
    SUPER_95(4, "Super 95", "#e654a0", "Shell Super FuelSave 95", null),
    SUPER_PLUS(5, "SuperPlus", "#29a5fa", null, "SuperPlus"),
    AUTOGAS(6, "Autogas", "#e12b49", "Shell Autogas (LPG)", "Autogas");

    private final short id;
    private final String name;
    private final Color color;
    private final String shellName, cleverName;

    Fuel(int id, String name, String color, String shellName, String cleverName) {
        this.id = (short) id;
        this.name = name;
        this.color = Color.decode(color);
        this.shellName = shellName;
        this.cleverName = cleverName;
    }

    public short getId() {
        return id;
    }

    public String toString() {
        return getName();
    }

    public static Fuel getById(int id) {
        for (Fuel fuel : values()) {
            if (fuel.id == id) {
                return fuel;
            }
        }
        return null;
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