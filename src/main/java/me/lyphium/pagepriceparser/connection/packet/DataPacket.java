package me.lyphium.pagepriceparser.connection.packet;

import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Packet;

import java.util.List;

public class DataPacket extends Packet {

    private static final long serialVersionUID = 6104171593145337744L;

    private final List<PriceData> data;

    public DataPacket(List<PriceData> data) {
        this.data = data;
    }

    public List<PriceData> getData() {
        return data;
    }

}