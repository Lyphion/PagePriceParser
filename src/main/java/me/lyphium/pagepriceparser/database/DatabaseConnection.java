package me.lyphium.pagepriceparser.database;

import lombok.Getter;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;

import java.sql.*;
import java.util.Date;
import java.util.*;
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

    private ExecutorService service;
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
            System.err.println("Already connected");
            return false;
        }

        try {
            // Checking if the MySQL Driver is available
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Creating database connection
            this.con = DriverManager.getConnection(
                    String.format(
                            "jdbc:mysql://%s:%d/%s?autoReconnect=true&serverTimezone=Europe/Berlin",
                            host, port, database
                    ),
                    username, password
            );

            // Creating async Threads if not already done
            if (service == null) {
                service = Executors.newCachedThreadPool();
            }

            System.out.println("Connected to database");
            return true;
        } catch (SQLException e) {
            System.err.println("Couldn't connect to database");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.err.println("No MySQL driver found");
            return false;
        }
    }

    public boolean disconnect() {
        if (!isConnected()) {
            System.err.println("Already disconnected from database");
            return false;
        }

        try {
            // Closing connection
            if (con != null) {
                con.close();
                con = null;
            }

            // Stopping async Threads
            if (service != null) {
                service.shutdown();
                service = null;
            }

            System.out.println("Disconnected from database");
            return true;
        } catch (SQLException e) {
            System.err.println("Error while disconnecting from database");
            return false;
        }
    }

    public List<PriceData> getPages() {
        if (!isConnected()) {
            System.err.println("No connection available");
            return null;
        }

        final List<PriceData> data = new ArrayList<>();

        final String execute = "SELECT id, name, url, address from pages;";
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
            System.err.println("No connection available");
            return false;
        }

        final String execute = "SELECT p.fuelid, p.time, p.value " +
                "FROM prices p " +
                "INNER JOIN fuels f on p.fuelid = f.id " +
                "WHERE p.pageid = ? AND p.time >= ? AND p.time <= ?;";

        final PreparedStatement statement = createStatement(execute);

        try {
            statement.setInt(1, data.getId());
            statement.setTimestamp(2, begin);
            statement.setTimestamp(3, end);
        } catch (SQLException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            close(null, statement);
            return false;
        }

        final ResultSet set = syncExecute(statement);

        try {
            while (set.next()) {
                final Fuel fuel = Fuel.getById(set.getInt("fuelid"));
                final long time = set.getTimestamp("time").getTime();
                final float value = set.getFloat("value");
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
            System.err.println("No connection available");
            return null;
        }

        final String execute = "SELECT id, url, address FROM pages WHERE LOWER(name) = LOWER(?) LIMIT 1;";
        final PreparedStatement statement = createStatement(execute);

        try {
            statement.setString(1, name);
        } catch (SQLException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            return null;
        }

        final ResultSet set = syncExecute(statement);

        try {
            if (!set.next()) {
                return null;
            }

            final int id = set.getInt("id");
            final String url = set.getString("url");
            final String address = set.getString("address");

            final PriceData data = new PriceData(id, name, url, address);
            loadPriceData(data, begin, end);

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(set, statement);
        }
    }

    public PriceData getPriceData(int id, Timestamp begin, Timestamp end) {
        if (!isConnected()) {
            System.err.println("No connection available");
            return null;
        }

        final String execute = "SELECT name, url, address FROM pages WHERE id = ? LIMIT 1;";
        final PreparedStatement statement = createStatement(execute);

        try {
            statement.setInt(1, id);
        } catch (SQLException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            close(null, statement);
            return null;
        }

        final ResultSet set = syncExecute(statement);

        try {
            if (!set.next()) {
                return null;
            }

            final String name = set.getString("name");
            final String url = set.getString("url");
            final String address = set.getString("address");

            final PriceData data = new PriceData(id, name, url, address);
            loadPriceData(data, begin, end);

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(set, statement);
        }
    }

    public List<PriceData> getPriceData(Fuel fuel, Timestamp begin, Timestamp end) {
        if (!isConnected()) {
            System.err.println("No connection available");
            return null;
        }

        final Map<Integer, PriceData> data = new HashMap<>();

        final String execute = "SELECT pa.id, pa.name, pa.url, pa.address, pr.time, pr.value " +
                "FROM prices pr INNER JOIN pages pa on pr.pageid = pa.id " +
                "WHERE pr.fuelid = ? AND pr.time >= ? AND pr.time <= ?;";

        final PreparedStatement statement = createStatement(execute);

        try {
            statement.setInt(1, fuel.getId());
            statement.setTimestamp(2, begin);
            statement.setTimestamp(3, end);
        } catch (SQLException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            close(null, statement);
            return null;
        }

        final ResultSet set = syncExecute(statement);

        try {
            while (set.next()) {
                final int id = set.getInt("id");
                final boolean existed = data.containsKey(id);

                final PriceData priceData;
                if (!existed) {
                    final String name = set.getString("name");
                    final String url = set.getString("url");
                    final String address = set.getString("address");

                    priceData = new PriceData(id, name, url, address);
                } else {
                    priceData = data.get(id);
                }

                final long time = set.getTimestamp("time").getTime();
                final float value = set.getFloat("value");

                priceData.addPrice(fuel, time, value);
                if (!existed) {
                    data.put(id, priceData);
                }
            }

            return new ArrayList<>(data.values());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(set, statement);
        }
    }

    public boolean savePriceData(List<PriceData> data) {
        if (!isConnected()) {
            System.err.println("No connection available");
            return false;
        }

        final long count = data.stream().mapToLong(pd -> pd.getPrices().values().size()).sum();

        if (count == 0) {
            return true;
        }

        final String update = "INSERT INTO prices (pageid, fuelid, time, value) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE value = VALUES(value);";

        final PreparedStatement statement = createStatement(update);

        try {
            long i = 0;
            final Timestamp time = new Timestamp(0);
            for (PriceData pd : data) {
                for (Entry<Fuel, Map<Long, Float>> fuelsEntry : pd.getPrices().entrySet()) {
                    for (Entry<Long, Float> timeEntry : fuelsEntry.getValue().entrySet()) {
                        time.setTime(timeEntry.getKey());

                        statement.setInt(1, pd.getId());
                        statement.setInt(2, fuelsEntry.getKey().getId());
                        statement.setTimestamp(3, time);
                        statement.setFloat(4, timeEntry.getValue());

                        statement.addBatch();
                        i++;

                        if (i % 1000 == 0 || i == count) {
                            syncExecuteBatch(statement);
                        }
                    }
                }
            }

            return true;
        } catch (SQLException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            return false;
        } finally {
            close(null, statement);
        }
    }

    public boolean addPage(PriceData data) {
        if (!isConnected()) {
            System.err.println("No connection available");
            return false;
        }

        if (getPriceData(data.getName(), new Timestamp(0), new Timestamp(0)) != null) {
            return false;
        }

        final String update = "INSERT INTO pages (name, url, address) VALUES(?, ?, ?);";
        final PreparedStatement statement = createStatement(update);

        try {
            statement.setString(1, data.getName());
            statement.setString(2, data.getUrl());
            statement.setString(3, data.getAddress());
        } catch (SQLException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            close(null, statement);
            return false;
        }

        update(statement);

        return true;
    }

    public boolean removePage(PriceData data) {
        if (!isConnected()) {
            System.err.println("No connection available");
            return false;
        }

        final String update;
        if (data.getId() > -1) {
            if (data.getName() != null) {
                update = "DELETE FROM pages WHERE id = ? AND LOWER(name) = LOWER(?);";
            } else {
                update = "DELETE FROM pages WHERE id = ?;";
            }
        } else if (data.getName() != null) {
            update = "DELETE FROM pages WHERE LOWER(name) = LOWER(?);";
        } else {
            return false;
        }

        final PreparedStatement statement = createStatement(update);

        try {
            int i = 1;
            if (data.getId() > -1) {
                statement.setInt(i++, data.getId());
            }
            if (data.getName() != null) {
                statement.setString(i, data.getName());
            }
        } catch (SQLException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            close(null, statement);
            return false;
        }

        return syncUpdate(statement) != 0;
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
            System.err.println("No connection available");
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
            System.err.println("No connection available");
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

    private int syncUpdate(PreparedStatement statement) {
        try {
            final int res = statement.executeUpdate();
            statement.close();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void update(PreparedStatement statement) {
        service.execute(() -> syncUpdate(statement));
    }

    private int[] syncExecuteBatch(PreparedStatement statement) {
        if (!isConnected()) {
            System.err.println("No connection available");
            return null;
        }

        try {
            return statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void executebatch(PreparedStatement statement) {
        service.execute(() -> syncExecuteBatch(statement));
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
            return o.toString().replace(',', '.');
        } else if (o instanceof Boolean) {
            return (Boolean) o ? "1" : "0";
        } else if (o instanceof Date) {
            return "'" + new Timestamp(((Date) o).getTime()).toString() + "'";
        }
        return "'" + o.toString() + "'";
    }

}