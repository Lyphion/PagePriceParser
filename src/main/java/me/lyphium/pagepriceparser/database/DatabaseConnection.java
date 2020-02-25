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

        final String execute = String.format(
                "SELECT p.fuelid, p.time, p.value " +
                        "FROM prices p " +
                        "INNER JOIN fuels f on p.fuelid = f.id " +
                        "WHERE p.pageid = %d AND p.time >= '%s' AND p.time <= '%s';",
                data.getId(), begin, end
        );

        final PreparedStatement statement = createStatement(execute);
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

        final String execute = String.format(
                "SELECT id, url, address FROM pages " +
                        "WHERE LOWER(name) = LOWER('%s') " +
                        "LIMIT 1;",
                name
        );

        final PreparedStatement statement = createStatement(execute);
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

        final String execute = String.format(
                "SELECT name, url, address FROM pages " +
                        "WHERE id = %d " +
                        "LIMIT 1;",
                id
        );

        final PreparedStatement statement = createStatement(execute);
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

        final String execute = String.format(
                "SELECT pa.id, pa.name, pa.url, pa.address, pr.time, pr.value " +
                        "FROM prices pr " +
                        "INNER JOIN pages pa on pr.pageid = pa.id " +
                        "WHERE pr.fuelid = %d AND pr.time >= '%s' AND pr.time <= '%s';",
                fuel.getId(), begin, end
        );

        final PreparedStatement statement = createStatement(execute);
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

        final List<String> priceData = new ArrayList<>();
        for (PriceData pd : data) {
            for (Entry<Fuel, Map<Long, Float>> fuelsEntry : pd.getPrices().entrySet()) {
                for (Entry<Long, Float> timeEntry : fuelsEntry.getValue().entrySet()) {
                    final String value = String.format(
                            "(%d, %d, '%s', %s)",
                            pd.getId(), fuelsEntry.getKey().getId(), new Timestamp(timeEntry.getKey()), toStringData(timeEntry.getValue())
                    );

                    priceData.add(value);
                }
            }
        }

        if (priceData.isEmpty()) {
            return true;
        }

        final String values = String.join(", ", priceData);
        final String update = String.format(
                "INSERT INTO prices (pageid, fuelid, time, value) " +
                        "VALUES %s " +
                        "ON DUPLICATE KEY UPDATE value = VALUES(value);",
                values
        );

        final PreparedStatement statement = createStatement(update);

        update(statement);

        return true;
    }

    public boolean addPage(PriceData data) {
        if (!isConnected()) {
            System.err.println("No connection available");
            return false;
        }

        if (getPriceData(data.getName(), new Timestamp(0), new Timestamp(0)) != null) {
            return false;
        }

        final String update = String.format(
                "INSERT INTO pages (name, url, address) " +
                        "VALUES('%s', '%s', '%s');",
                data.getName(), data.getUrl(), data.getAddress()
        );

        final PreparedStatement statement = createStatement(update);

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
                update = String.format(
                        "DELETE FROM pages WHERE id = %d AND LOWER(name) = LOWER('%s');",
                        data.getId(), data.getName()
                );
            } else {
                update = String.format(
                        "DELETE FROM pages WHERE id = %d;",
                        data.getId()
                );
            }
        } else if (data.getName() != null) {
            update = String.format(
                    "DELETE FROM pages WHERE LOWER(name) = LOWER('%s');",
                    data.getName()
            );
        } else {
            return false;
        }

        final PreparedStatement statement = createStatement(update);

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