package me.lyphium.pagepriceparser.connection.packet;

import me.lyphium.pagepriceparser.connection.RequestType;

public class DataRequestPacket extends Packet {

    private static final long serialVersionUID = -5673512560358717006L;

    private final RequestType[] types;
    private final Object[] data;

    public DataRequestPacket(RequestType[] types, Object[] data) {
        super(true);
        this.types = types;
        this.data = data;
    }

    public RequestType[] getTypes() {
        return types;
    }

    public Object[] getData() {
        return data;
    }

}