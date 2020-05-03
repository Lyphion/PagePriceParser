package me.lyphium.pagepriceparser.parser;

import lombok.Getter;

import java.awt.*;

@Getter
public enum Fuel {

    DIESEL(0, "Diesel", "#4b7bec", new String[]{
            "Shell Diesel FuelSave", "Diesel"
    }),
    LKW_DIESEL(1, "LKW-Diesel", "#0fb9b1", new String[]{
            "Shell Truck Diesel", "LKW-Diesel"
    }),
    SUPER_E10(2, "Super E10", "#20bf6b", new String[]{
            "Shell Super FuelSave E10", "Super E10"
    }),
    SUPER_E5(3, "Super E5", "#eb3b5a", new String[]{
            "Shell Super FuelSave E5", "Super E5"
    }),
    SUPER_95(4, "Super 95", "#fed330", new String[]{
            "Shell Super FuelSave 95"
    }),
    SUPER_PLUS(5, "SuperPlus", "#fa8231", new String[]{
            "SuperPlus", "Esso Superplus", "ARAL Ultimate 102", "Shell V-Power Racing"
    }),
    AUTOGAS(6, "Autogas", "#8854d0", new String[]{
            "Shell Autogas (LPG)", "Autogas"
    });

    private final short id;
    private final String name;
    private final Color color;
    private final String[] names;

    Fuel(int id, String name, String color, String[] names) {
        this.id = (short) id;
        this.name = name;
        this.color = Color.decode(color);
        this.names = names;
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
            for (String s : fuel.names) {
                if (s.equalsIgnoreCase(name)) {
                    return fuel;
                }
            }
        }
        return null;
    }

}