package me.lyphium.pagepriceparser.parser;

public enum PriceType {

    DIESEL,
    BENZIN,
    AUTOGAS;

    public String getName() {
        return name().substring(0, 1) + name().substring(1).toUpperCase();
    }

}