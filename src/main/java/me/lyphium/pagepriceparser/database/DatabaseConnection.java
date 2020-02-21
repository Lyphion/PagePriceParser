package me.lyphium.pagepriceparser.database;

import lombok.Getter;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DatabaseConnection {

    @Getter
    private final String host, database;
    @Getter
    private final int port;

    private final String username, password;

    private final ExecutorService service = Executors.newCachedThreadPool();
    private Connection con;

    public DatabaseConnection(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public boolean connect() {
        if (isConnected()) {
            System.err.println("Already connected!");
            return false;
        }

        try {
            this.con = DriverManager.getConnection(
                    String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true", host, port, database),
                    username, password
            );
            System.out.println("Connected to database!");
            return true;
        } catch (SQLException e) {
            System.err.println("Couldn't connect to database!");
            return false;
        }
    }

    public boolean disconnect() {
        if (!isConnected()) {
            System.err.println("Already disconnected from database!");
            return false;
        }

        try {
            con.close();
            con = null;
            service.shutdown();
            System.out.println("Disconnected from database!");
            return true;
        } catch (SQLException e) {
            System.err.println("Error while disconnecting from database!");
            return false;
        }
    }

    public boolean isConnected() {
        try {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private PreparedStatement createStatement(String statement) {
        if (!isConnected()) {
            System.err.println("No connection available!");
            return null;
        }

        try {
            return con.prepareStatement(statement);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ResultSet syncExecute(PreparedStatement statement) {
        if (!isConnected()) {
            System.err.println("No connection available!");
            return null;
        }

        try {
            return statement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Future<ResultSet> execute(PreparedStatement statement) {
        return service.submit(() -> syncExecute(statement));
    }

    private void syncUpdate(PreparedStatement statement) {
        try {
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void update(PreparedStatement statement) {
        service.execute(() -> syncUpdate(statement));
    }

    private void close(ResultSet set, PreparedStatement statement) {
        try {
            if (set != null) {
                set.close();
            }
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String toStringData(Object o) {
        if (o == null) {
            return "NULL";
        } else if (o instanceof Number) {
            return o.toString();
        } else if (o instanceof Boolean) {
            return (Boolean) o ? "1" : "0";
        } else if (o instanceof Date) {
            return "'" + new Timestamp(((Date) o).getTime()).toString() + "'";
        }
        return "'" + o.toString() + "'";
    }

}
