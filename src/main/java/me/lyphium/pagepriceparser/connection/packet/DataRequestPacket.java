package me.lyphium.pagepriceparser.connection.packet;

public class DataRequestPacket extends Packet {

    private static final long serialVersionUID = -5673512560358717006L;

    private final String[] names;
    private final Object[] data;

    public DataRequestPacket(String[] names, Object[] data) {
        super(true);
        this.names = names;
        this.data = data;
    }

    public String[] getName() {
        return names;
    }

    public Object[] getData() {
        return data;
    }

}