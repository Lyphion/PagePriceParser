package me.lyphium.pagepriceparser.database;

import lombok.Getter;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.con = DriverManager.getConnection(
                    String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true&serverTimezone=Europe/Berlin", host, port, database),
                    username, password
            );
            System.out.println("Connected to database!");
            return true;
        } catch (SQLException e) {
            System.err.println("Couldn't connect to database!");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("No MySQL driver found!");
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

    public List<PriceData> getPages() {
        final List<PriceData> data = new ArrayList<>();

        if (!isConnected()) {
            System.err.println("No connection available!");
            return data;
        }

        final String execute = "SELECT * from pages;";
        final PreparedStatement statement = createStatement(execute);
        final ResultSet set = syncExecute(statement);

        try {
            while (set.next()) {
                final int id = set.getInt("id");
                final String name = set.getString("name");
                final String url = set.getString("url");
                final String address = set.getString("address");

                final PriceData priceData = new PriceData(id, name, url, address);
                data.add(priceData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(set, statement);
        }

        return data;
    }

    public boolean loadPriceData(PriceData data, Timestamp begin, Timestamp end) {
        if (!isConnected()) {
            System.err.println("No connection available!");
            return false;
        }

        final String execute = "SELECT p.fuelid, p.time, p.value " +
                "FROM prices p " +
                "INNER JOIN fuels f on p.fuelid = f.id " +
                "WHERE p.pageid = " + data.getId() + " AND p.time >= '" + begin + "' AND p.time <= '" + end + "';";
        final PreparedStatement statement = createStatement(execute);
        final ResultSet set = syncExecute(statement);

        try {
            while (set.next()) {
                final Fuel fuel = Fuel.getByID(set.getInt("p.fuelid"));
                final long time = set.getTimestamp("p.time").getTime();
                final float value = set.getFloat("p.value");
                data.addPrice(fuel, time, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            close(set, statement);
        }
    }

    public PriceData getPriceData(String name, Timestamp begin, Timestamp end) {
        if (!isConnected()) {
            System.err.println("No connection available!");
            return null;
        }

        // TODO
        return null;
    }

    public PriceData getPriceData(int id, Timestamp begin, Timestamp end) {
        if (!isConnected()) {
            System.err.println("No connection available!");
            return null;
        }

        // TODO
        return null;
    }

    public boolean savePriceData(List<PriceData> data) {
        if (!isConnected()) {
            System.err.println("No connection available!");
            return false;
        }

        final List<String> priceData = new ArrayList<>();
        for (PriceData pd : data) {
            for (Entry<Fuel, Map<Long, Float>> fuelsEntry : pd.getPrices().entrySet()) {
                for (Entry<Long, Float> timeEntry : fuelsEntry.getValue().entrySet()) {
                    final String value = "(" + pd.getId() + ", " + fuelsEntry.getKey().getId() + ", " +
                            toStringData(new Timestamp(timeEntry.getKey())) + ", " + timeEntry.getValue() + ")";
                    priceData.add(value);
                }
            }
        }

        if (priceData.isEmpty()) {
            return true;
        }

        final String values = String.join(", ", priceData);
        final String update = "INSERT INTO prices (pageid, fuelid, time, value) " +
                "VALUES " + values + " " +
                "ON DUPLICATE KEY UPDATE value = VALUES(value);";
        final PreparedStatement statement = createStatement(update);

        update(statement);
        return true;
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
        } else if (o instanceof Date && !(o instanceof Timestamp)) {
            return "'" + new Timestamp(((Date) o).getTime()).toString() + "'";
        }
        return "'" + o.toString() + "'";
    }

}
