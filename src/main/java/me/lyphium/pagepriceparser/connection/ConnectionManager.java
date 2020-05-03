package me.lyphium.pagepriceparser.connection;

import lombok.Getter;
import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.connection.packet.DataPacket;
import me.lyphium.pagepriceparser.connection.packet.DataRequestPacket;
import me.lyphium.pagepriceparser.connection.packet.InvalidRequestPacket;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Packet;
import me.lyphium.pagepriceparser.utils.PriceMap;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConnectionManager extends Thread {

    public static final short DEFAULT_PORT = 14703;

    @Getter
    private final int port;

    private ServerSocket server;

    public ConnectionManager(int port) {
        this.port = port;

        setName("ConnectionManager");
        setDaemon(true);
    }

    @Override
    public void run() {
        if (port < 0) {
            System.out.println("Client Manager disabled");
            return;
        }

        try {
            // Setting up SocketServer to receive Client requests
            this.server = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("Couldn't setup ServerSocket");
        }

        System.out.println("Started Client Manager");

        // Checking if the bot is still running
        while (Bot.getInstance().isRunning() && isValid()) {
            try {
                // Waiting for Client requests
                final Socket socket = server.accept();

                // Set maximum waiting time for input
                socket.setSoTimeout(500);
                final InputStream inputStream = socket.getInputStream();
                final ObjectInputStream in = new ObjectInputStream(inputStream);

                // Read Request
                final Object obj = in.readObject();

                // Check if the Request is a valid Packet
                if (obj instanceof Packet) {
                    // Handle Packet
                    final Packet result = handle((Packet) obj);

                    // Send back Answer if needed
                    if (result != null) {
                        final OutputStream outputStream = socket.getOutputStream();
                        final ObjectOutputStream out = new ObjectOutputStream(outputStream);
                        out.writeObject(result);
                    }
                }

                // Close connection
                socket.close();
            } catch (ClassNotFoundException | ObjectStreamException e) {
                // Thrown when data is invalid or corrupted
                System.err.println("Invalid packet received");
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

        if (!isValid()) {
            return;
        }

        try {
            // Close ServerSocket
            server.close();
            server = null;

            System.out.println("Shut down Client Manager");
        } catch (IOException e) {
            System.err.println("Error while shutting down Client Manager");
        }
    }

    private Packet handle(Packet packet) {
        // Handle each Packet differently

        if (packet instanceof DataRequestPacket) {
            final DataRequestPacket request = (DataRequestPacket) packet;
            final DatabaseConnection database = Bot.getInstance().getDatabase();

            try {
                final RequestType[] types = request.getTypes();
                final Object[] data = request.getData();

                // Check if the Packet has data
                if (types == null || types.length == 0) {
                    return new InvalidRequestPacket("Types must contain at least: id, name or fuel");
                }

                // Check if the types and data arrays have equal length
                if (types.length != data.length) {
                    return new InvalidRequestPacket("Length of types and data must be equal");
                }

                int id = -1;
                String name = null;
                Fuel fuel = null;
                final Timestamp begin = new Timestamp(0);
                final Timestamp end = new Timestamp(System.currentTimeMillis());

                // Parse arguments by type
                for (int i = 0; i < types.length; i++) {
                    final RequestType type = types[i];

                    if (type == RequestType.ID) {
                        id = (int) data[i];
                        if (id < 0) {
                            return new InvalidRequestPacket("Id must be at least 0");
                        }
                    } else if (type == RequestType.NAME) {
                        name = (String) data[i];
                    } else if (type == RequestType.FUEL) {
                        fuel = (Fuel) data[i];
                    } else if (type == RequestType.TIME_BEGIN) {
                        begin.setTime((long) data[i]);
                    } else if (type == RequestType.TIME_END) {
                        end.setTime((long) data[i]);
                    }
                }

                // If id is set
                if (id > -1) {
                    // Get PriceData from database with id
                    final PriceData priceData = database.getPriceData(id, begin, end);

                    // Check if data exists
                    if (priceData == null) {
                        return new InvalidRequestPacket("No pricedata found");
                    }

                    // Check if the given is equal with the request name
                    if (name != null && !priceData.getName().equals(name)) {
                        return new InvalidRequestPacket("Name and id don't match");
                    }

                    // Filter if fuel is set
                    if (fuel != null) {
                        final PriceData newPriceData = new PriceData(priceData.getId(), priceData.getName(), priceData.getUrl(), priceData.getAddress(), priceData.getColor());
                        final PriceMap prices = priceData.getPrices(fuel);

                        for (int i = 0; i < prices.size(); i++) {
                            newPriceData.addPrice(fuel, prices.getKey(i), prices.get(i));
                        }

                        // Send back filtered PriceData
                        return new DataPacket(new ArrayList<>(Collections.singletonList(newPriceData)));
                    } else {
                        // Send back PriceData
                        return new DataPacket(new ArrayList<>(Collections.singletonList(priceData)));
                    }
                }
                // If name is set and not id
                else if (name != null) {
                    // Get PriceData from database with name
                    final PriceData priceData = database.getMostSimilarPriceData(name, begin, end);

                    // Check if data exists
                    if (priceData == null) {
                        return new InvalidRequestPacket("No pricedata found");
                    }

                    // Filter if fuel is set
                    if (fuel != null) {
                        final PriceData newPriceData = new PriceData(priceData.getId(), priceData.getName(), priceData.getUrl(), priceData.getAddress(), priceData.getColor());
                        final PriceMap prices = priceData.getPrices(fuel);

                        for (int i = 0; i < prices.size(); i++) {
                            newPriceData.addPrice(fuel, prices.getKey(i), prices.get(i));
                        }

                        // Send back filtered PriceData
                        return new DataPacket(new ArrayList<>(Collections.singletonList(newPriceData)));
                    } else {
                        // Send back PriceData
                        return new DataPacket(new ArrayList<>(Collections.singletonList(priceData)));
                    }
                } else if (fuel != null) {
                    // Get List of PriceData from database with fuel
                    final List<PriceData> priceData = database.getPriceData(fuel, begin, end);

                    // Send back List of PriceData
                    return new DataPacket(priceData);
                } else {
                    // Request must contain at least: id, name or fuel
                    return new InvalidRequestPacket("Types must contain at least: id, name or fuel");
                }
            } catch (Exception e) {
                System.err.println("Invalid packet received");
                return new InvalidRequestPacket("Invalid packet received");
            }
        }

        return null;
    }

    private boolean isValid() {
        return server != null && !server.isClosed();
    }

}