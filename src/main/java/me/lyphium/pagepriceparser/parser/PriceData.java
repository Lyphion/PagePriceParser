package me.lyphium.pagepriceparser.parser;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

@Getter
public class PriceData {

    private final int id;
    private final String name;
    private final String url;

    @Getter(AccessLevel.NONE)
    private final Map<PriceType, Map<Long, Double>> prices;

    public PriceData(int id, String name, String url, Map<PriceType, Map<Long, Double>> prices) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.prices = prices;
    }

    public Map<Long, Double> getPrices(PriceType type) {
        return Collections.unmodifiableMap(prices.get(type));
    }

}