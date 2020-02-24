package me.lyphium.pagepriceparser.connection;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.connection.packet.DataPacket;
import me.lyphium.pagepriceparser.connection.packet.DataRequestPacket;
import me.lyphium.pagepriceparser.connection.packet.InvalidRequestPacket;
import me.lyphium.pagepriceparser.connection.packet.Packet;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

public class ConnectionManager extends Thread {

    private final int port;

    private ServerSocket server;

    public ConnectionManager(int port) {
        this.port = port;

        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Couldn't setup ServerSocket!");
        }
    }

    @Override
    public void run() {
        while (Bot.getInstance().isRunning()) {
            try {
                final Socket socket = server.accept();
                socket.setSoTimeout(500);
                final InputStream inputStream = socket.getInputStream();
                final ObjectInputStream in = new ObjectInputStream(inputStream);

                final Object obj = in.readObject();

                if (obj instanceof Packet) {
                    final Packet result = handle((Packet) obj);
                    if (result != null) {
                        final OutputStream outputStream = socket.getOutputStream();
                        final ObjectOutputStream out = new ObjectOutputStream(outputStream);
                        out.writeObject(result);
                    }
                }

                socket.close();
            } catch (ClassNotFoundException | ObjectStreamException e) {
                // Thrown when data is invalid or corrupted
                System.err.println("Invalid packet received!");
            } catch (SocketTimeoutException e) {
                // Thrown when socket is timing out or isn't sending any packets
            } catch (SocketException e) {
                // Thrown when the socket is closing, that means ConnectionManager is shutting down
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void cancel() {
        interrupt();

        try {
            server.close();
            server = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Packet handle(Packet packet) {
        if (packet instanceof DataRequestPacket) {
            final DataRequestPacket request = (DataRequestPacket) packet;
            final DatabaseConnection database = Bot.getInstance().getDatabase();

            try {
                RequestType[] types = request.getTypes();
                Object[] data = request.getData();

                if (types == null || types.length == 0) {
                    return new InvalidRequestPacket("Types must contain at least: id, name or fuel");
                }

                if (types.length != data.length) {
                    return new InvalidRequestPacket("Length of types and data must be equal");
                }

                int id = -1;
                String name = null;
                Fuel fuel = null;
                long begin = 0;
                long end = System.currentTimeMillis();

                for (int i = 0; i < types.length; i++) {
                    final RequestType type = types[i];

                    if (type == RequestType.ID) {
                        id = (int) data[i];
                    } else if (type == RequestType.NAME) {
                        name = (String) data[i];
                    } else if (type == RequestType.FUEL) {
                        fuel = (Fuel) data[i];
                    } else if (type == RequestType.TIME_BEGIN) {
                        begin = (long) data[i];
                    } else if (type == RequestType.TIME_END) {
                        end = (long) data[i];
                    }
                }

                if (id > -1) {
                    final PriceData priceData = database.getPriceData(id, new Timestamp(begin), new Timestamp(end));
                    if (!priceData.getName().equals(name)) {
                        return new InvalidRequestPacket("Name and id don't match");
                    }
                    if (fuel != null) {
                        final PriceData newPriceData = new PriceData(priceData.getId(), priceData.getName(), priceData.getUrl(), priceData.getAddress());

                        for (Entry<Long, Float> entry : priceData.getPrices(fuel).entrySet()) {
                            newPriceData.addPrice(fuel, entry.getKey(), entry.getValue());
                        }

                        return new DataPacket(new ArrayList<>(Collections.singletonList(newPriceData)));
                    }
                } else if (name != null) {
                    final PriceData priceData = database.getPriceData(name, new Timestamp(begin), new Timestamp(end));

                    if (fuel != null) {
                        final PriceData newPriceData = new PriceData(priceData.getId(), priceData.getName(), priceData.getUrl(), priceData.getAddress());

                        for (Entry<Long, Float> entry : priceData.getPrices(fuel).entrySet()) {
                            newPriceData.addPrice(fuel, entry.getKey(), entry.getValue());
                        }

                        return new DataPacket(new ArrayList<>(Collections.singletonList(newPriceData)));
                    }
                } else if (fuel != null) {
                    final List<PriceData> priceData = database.getPriceData(fuel, new Timestamp(begin), new Timestamp(end));

                    return new DataPacket(priceData);
                } else {
                    return new InvalidRequestPacket("Types must contain at least: id, name or fuel");
                }
            } catch (Exception e) {
                System.err.println("Invalid packet received!");
                return new InvalidRequestPacket("Invalid Packet received");
            }
        }

        return null;
    }

    private boolean isValid() {
        return server != null && !server.isClosed();
    }

}