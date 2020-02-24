package me.lyphium.pagepriceparser.connection.packet;

public class InvalidRequestPacket extends Packet {

    private static final long serialVersionUID = -1712436439560792208L;

    private final String message;

    public InvalidRequestPacket(String message) {
        super(false);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}