package me.lyphium.pagepriceparser.database;

import com.zaxxer.hikari.HikariDataSource;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.PriceMap;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DatabaseConnection {

    private final HikariDataSource source;

    private final ExecutorService service;

    public DatabaseConnection(String host, int port, String database, String username, String password) {
        this.source = new HikariDataSource();

        source.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", host, port, database));
        source.setUsername(username);
        source.setPassword(password);

        source.addDataSourceProperty("serverTimezone", "Europe/Berlin");
        source.addDataSourceProperty("connectionTimeout", 5000);
        source.addDataSourceProperty("cachePrepStmts", true);
        source.addDataSourceProperty("prepStmtCacheSize", 250);
        source.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        source.addDataSourceProperty("useServerPrepStmts", true);
        source.addDataSourceProperty("useLocalSessionState", true);
        source.addDataSourceProperty("rewriteBatchedStatements", true);
        source.addDataSourceProperty("cacheResultSetMetadata", true);
        source.addDataSourceProperty("cacheServerConfiguration", true);
        source.addDataSourceProperty("elideSetAutoCommits", true);
        source.addDataSourceProperty("maintainTimeStats", false);

        this.service = Executors.newCachedThreadPool();
    }

    public void stop() {
        service.shutdown();
        source.close();

        System.out.println("Shut down Database Manager");
    }

    public List<PriceData> getPages() {
        final String sql = "SELECT id, name, url, address from pages;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql);
             ResultSet set = syncExecute(statement)) {

            final List<PriceData> data = new ArrayList<>();

            while (set.next()) {
                final int id = set.getInt("id");
                final String name = set.getString("name");
                final String url = set.getString("url");
                final String address = set.getString("address");

                final PriceData priceData = new PriceData(id, name, url, address);
                data.add(priceData);
            }

            return data;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean loadPriceData(PriceData data, Timestamp begin, Timestamp end) {
        final String sql = "SELECT p.fuelid, p.time, p.value " +
                "FROM prices p " +
                "INNER JOIN fuels f on p.fuelid = f.id " +
                "WHERE p.pageid = ? AND p.time >= ? AND p.time <= ?;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, data.getId());
            statement.setTimestamp(2, begin);
            statement.setTimestamp(3, end);

            try (ResultSet set = syncExecute(statement)) {
                while (set.next()) {
                    final Fuel fuel = Fuel.getById(set.getInt("fuelid"));
                    final long time = set.getTimestamp("time").getTime();
                    final float value = set.getFloat("value");
                    data.addPrice(fuel, time, value);
                }
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public PriceData getPriceData(String name, Timestamp begin, Timestamp end) {
        final String sql = "SELECT id, url, address FROM pages WHERE LOWER(name) = LOWER(?) LIMIT 1;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setString(1, name);

            try (ResultSet set = syncExecute(statement)) {
                if (!set.next()) {
                    return null;
                }

                final int id = set.getInt("id");
                final String url = set.getString("url");
                final String address = set.getString("address");

                final PriceData data = new PriceData(id, name, url, address);
                loadPriceData(data, begin, end);

                return data;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PriceData getPriceData(int id, Timestamp begin, Timestamp end) {
        final String sql = "SELECT name, url, address FROM pages WHERE id = ? LIMIT 1;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet set = syncExecute(statement)) {
                if (!set.next()) {
                    return null;
                }

                final String name = set.getString("name");
                final String url = set.getString("url");
                final String address = set.getString("address");

                final PriceData data = new PriceData(id, name, url, address);
                loadPriceData(data, begin, end);

                return data;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PriceData> getPriceData(Fuel fuel, Timestamp begin, Timestamp end) {
        final String sql = "SELECT pa.id, pa.name, pa.url, pa.address, pr.time, pr.value " +
                "FROM prices pr INNER JOIN pages pa on pr.pageid = pa.id " +
                "WHERE pr.fuelid = ? AND pr.time >= ? AND pr.time <= ?;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, fuel.getId());
            statement.setTimestamp(2, begin);
            statement.setTimestamp(3, end);

            try (ResultSet set = syncExecute(statement)) {
                final Map<Integer, PriceData> data = new HashMap<>();

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
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean savePriceData(List<PriceData> data) {
        final long count = data.stream().mapToLong(pd -> pd.getPrices().values().size()).sum();
        if (count == 0) {
            return true;
        }

        final String sql = "INSERT INTO prices (pageid, fuelid, time, value) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE value = VALUES(value);";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            long i = 0;
            final Timestamp time = new Timestamp(0);
            for (PriceData pd : data) {
                for (Entry<Fuel, PriceMap> fuelsEntry : pd.getPrices().entrySet()) {
                    final PriceMap priceMap = fuelsEntry.getValue();
                    for (long t : priceMap.keySet()) {
                        time.setTime(t);

                        statement.setInt(1, pd.getId());
                        statement.setInt(2, fuelsEntry.getKey().getId());
                        statement.setTimestamp(3, time);
                        statement.setFloat(4, priceMap.get(t));

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
            e.printStackTrace();
            return false;
        }
    }

    public boolean addPage(PriceData data) {
        if (getPriceData(data.getName(), new Timestamp(0), new Timestamp(0)) != null) {
            return false;
        }

        final String sql = "INSERT INTO pages (name, url, address) VALUES(?, ?, ?);";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setString(1, data.getName());
            statement.setString(2, data.getUrl());
            statement.setString(3, data.getAddress());

            update(statement);

            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removePage(PriceData data) {
        final String sql;
        if (data.getId() > -1) {
            if (data.getName() != null) {
                sql = "DELETE FROM pages WHERE id = ? AND LOWER(name) = LOWER(?);";
            } else {
                sql = "DELETE FROM pages WHERE id = ?;";
            }
        } else if (data.getName() != null) {
            sql = "DELETE FROM pages WHERE LOWER(name) = LOWER(?);";
        } else {
            return false;
        }

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            int i = 1;
            if (data.getId() > -1) {
                statement.setInt(i++, data.getId());
            }
            if (data.getName() != null) {
                statement.setString(i, data.getName());
            }

            return syncUpdate(statement) != 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isConnected() {
        try (Connection con = source.getConnection()) {
            return con.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private ResultSet syncExecute(PreparedStatement statement) {
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
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void update(PreparedStatement statement) {
        service.execute(() -> syncUpdate(statement));
    }

    private int[] syncExecuteBatch(PreparedStatement statement) {
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

}