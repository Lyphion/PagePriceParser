package me.lyphium.pagepriceparser.parser;

import lombok.Getter;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Getter
public class PriceData {

    private final int id;
    private final String name;
    private final String url;
    private final String address;

    private final Map<Fuel, Map<Long, Float>> prices;

    public PriceData(int id, String name, String url, String address, Map<Fuel, Map<Long, Float>> prices) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.address = address;
        this.prices = prices;
    }

    public PriceData(int id, String name, String url, String location) {
        this(id, name, url, location, new EnumMap<>(Fuel.class));
    }

    public Map<Fuel, Float> getPrices(long time) {
        final Map<Fuel, Float> prices = new HashMap<>();

        for (Entry<Fuel, Map<Long, Float>> entry : this.prices.entrySet()) {
            if (entry.getValue().containsKey(time)) {
                prices.put(entry.getKey(), entry.getValue().get(time));
            }
        }

        return prices;
    }

    public Map<Long, Float> getPrices(Fuel fuel) {
        return prices.get(fuel);
    }

    public float getPrice(Fuel fuel, long time) {
        if (!prices.containsKey(fuel)) {
            return -1F;
        }

        return prices.get(fuel).getOrDefault(time, -1F);
    }

    public void addPrice(Fuel fuel, long time, float value) {
        if (!prices.containsKey(fuel)) {
            prices.put(fuel, new HashMap<>());
        }
        prices.get(fuel).put(time, value);
    }

}