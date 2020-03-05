package me.lyphium.pagepriceparser.parser;

import lombok.Getter;
import me.lyphium.pagepriceparser.utils.PriceMap;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@Getter
public class PriceData implements Serializable {

    private static final long serialVersionUID = 6726811421413544466L;

    private final int id;
    private final String name;
    private final String url;
    private final String address;

    private final Map<Fuel, PriceMap> prices;

    public PriceData(int id, String name, String url, String address, Map<Fuel, PriceMap> prices) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.address = address;
        this.prices = prices;
    }

    public PriceData(int id, String name, String url, String address) {
        this(id, name, url, address, new EnumMap<>(Fuel.class));
    }

    public Map<Fuel, Float> getPrices(long time) {
        final Map<Fuel, Float> prices = new HashMap<>();

        for (Entry<Fuel, PriceMap> entry : this.prices.entrySet()) {
            if (entry.getValue().containsKey(time)) {
                prices.put(entry.getKey(), entry.getValue().get(time));
            }
        }

        return prices;
    }

    public PriceMap getPrices(Fuel fuel) {
        return prices.get(fuel);
    }

    public float getPrice(Fuel fuel, long time) {
        return hasPrice(fuel, time) ? prices.get(fuel).get(time) : -1;
    }

    public boolean hasPrice(Fuel fuel, long time) {
        if (!prices.containsKey(fuel)) {
            return false;
        }
        return prices.get(fuel).containsKey(time);
    }

    public void addPrice(Fuel fuel, long time, float value) {
        if (!prices.containsKey(fuel)) {
            prices.put(fuel, new PriceMap());
        }
        prices.get(fuel).put(time, value);
    }

    @Override
    public String toString() {
        return "PriceData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", address='" + address + '\'' +
                ", prices=" + prices +
                '}';
    }

}