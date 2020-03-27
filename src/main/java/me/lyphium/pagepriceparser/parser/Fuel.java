package me.lyphium.pagepriceparser.parser;

import lombok.Getter;

import java.awt.*;

@Getter
public enum Fuel {

    DIESEL(0, "Diesel", "#4b7bec", "Shell Diesel FuelSave", "Diesel"),
    LKW_DIESEL(1, "LKW-Diesel", "#0fb9b1", "Shell Truck Diesel", "LKW-Diesel"),
    SUPER_E10(2, "Super E10", "#20bf6b", "Shell Super FuelSave E10", "Super E10"),
    SUPER_E5(3, "Super E5", "#eb3b5a", "Shell Super FuelSave E5", "Super E5"),
    SUPER_95(4, "Super 95", "#fed330", "Shell Super FuelSave 95", null),
    SUPER_PLUS(5, "SuperPlus", "#fa8231", null, "SuperPlus"),
    AUTOGAS(6, "Autogas", "#8854d0", "Shell Autogas (LPG)", "Autogas");

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