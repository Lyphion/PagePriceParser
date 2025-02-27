package me.lyphium.pagepriceparser.utils;

import lombok.Data;

@Data
public class Pair<T, U> {

    private T first;
    private U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

}
