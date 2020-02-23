package me.lyphium.pagepriceparser.connection;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.connection.packet.DataRequestPacket;
import me.lyphium.pagepriceparser.connection.packet.Packet;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

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
            } catch (ClassNotFoundException | ObjectStreamException e) {
                // Thrown when data is invalid or corrupted
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

            // TODO Add packet handle
        }

        return null;
    }

    private boolean isValid() {
        return server != null && !server.isClosed();
    }

}
