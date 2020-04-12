package me.lyphium.pagepriceparser.database;

import com.zaxxer.hikari.HikariDataSource;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Pair;
import me.lyphium.pagepriceparser.utils.PriceMap;
import me.lyphium.pagepriceparser.utils.Utils;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DatabaseConnection {

    private final HikariDataSource source;

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
    }

    public void stop() {
        source.close();

        System.out.println("Shut down Database Manager");
    }

    public List<PriceData> getPages() {
        final String sql = "SELECT id, name, url, address, color from pages;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql);
             ResultSet set = statement.executeQuery()) {

            final List<PriceData> data = new ArrayList<>();

            while (set.next()) {
                final int id = set.getInt("id");
                final String name = set.getString("name");
                final String url = set.getString("url");
                final String address = set.getString("address");
                final Color color = Color.decode(set.getString("color"));

                final PriceData priceData = new PriceData(id, name, url, address, color);
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
                "WHERE p.pageid = ? AND p.time BETWEEN ? AND ?;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, data.getId());
            statement.setTimestamp(2, begin);
            statement.setTimestamp(3, end);

            try (ResultSet set = statement.executeQuery()) {
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
        final String sql = "SELECT id, url, address, color FROM pages WHERE LOWER(name) = LOWER(?) LIMIT 1;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setString(1, name);

            try (ResultSet set = statement.executeQuery()) {
                if (!set.next()) {
                    return null;
                }

                final int id = set.getInt("id");
                final String url = set.getString("url");
                final String address = set.getString("address");
                final Color color = Color.decode(set.getString("color"));

                final PriceData data = new PriceData(id, name, url, address, color);
                loadPriceData(data, begin, end);

                return data;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PriceData getMostSimilarPriceData(String name, Timestamp begin, Timestamp end) {
        final List<PriceData> pages = getPages();

        if (pages == null || pages.isEmpty()) {
            return null;
        }

        name = name.toLowerCase();

        int minDis = Integer.MAX_VALUE;
        PriceData data = null;
        for (PriceData page : pages) {
            final int dis = Utils.distance(name, page.getName().toLowerCase());

            if (dis < minDis) {
                data = page;
                minDis = dis;
            }
        }

        loadPriceData(data, begin, end);

        return data;
    }

    public PriceData getPriceData(int id, Timestamp begin, Timestamp end) {
        final String sql = "SELECT name, url, address, color FROM pages WHERE id = ? LIMIT 1;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet set = statement.executeQuery()) {
                if (!set.next()) {
                    return null;
                }

                final String name = set.getString("name");
                final String url = set.getString("url");
                final String address = set.getString("address");
                final Color color = Color.decode(set.getString("color"));

                final PriceData data = new PriceData(id, name, url, address, color);
                loadPriceData(data, begin, end);

                return data;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PriceData> getPriceData(Fuel fuel, Timestamp begin, Timestamp end) {
        final String sql = "SELECT pa.id, pa.name, pa.url, pa.address, pa.color, pr.time, pr.value " +
                "FROM prices pr INNER JOIN pages pa on pr.pageid = pa.id " +
                "WHERE pr.fuelid = ? AND pr.time BETWEEN ? AND ?;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, fuel.getId());
            statement.setTimestamp(2, begin);
            statement.setTimestamp(3, end);

            try (ResultSet set = statement.executeQuery()) {
                final Map<Integer, PriceData> data = new HashMap<>();

                while (set.next()) {
                    final int id = set.getInt("id");
                    final boolean existed = data.containsKey(id);

                    final PriceData priceData;
                    if (!existed) {
                        final String name = set.getString("name");
                        final String url = set.getString("url");
                        final String address = set.getString("address");
                        final Color color = Color.decode(set.getString("color"));

                        priceData = new PriceData(id, name, url, address, color);
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
                            statement.executeBatch();
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

        final String sql = "INSERT INTO pages (name, url, address, color) VALUES (?, ?, ?, ?);";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setString(1, data.getName());
            statement.setString(2, data.getUrl());
            statement.setString(3, data.getAddress());

            final Color color = data.getColor();
            statement.setString(4, String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));

            return statement.executeUpdate() != 0;
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

            return statement.executeUpdate() != 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Pair<String, Object>> getDatabaseInformation() {
        final String sql = "SELECT (SELECT COUNT(name) FROM pages) as pages, " +
                "(SELECT COUNT(id) FROM prices) as prices, " +
                "(SELECT time FROM prices ORDER BY time LIMIT 1) as first, " +
                "(SELECT time FROM prices ORDER BY time DESC LIMIT 1) as last;";

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(sql);
             ResultSet set = statement.executeQuery()) {
            final List<Pair<String, Object>> info = new ArrayList<>();

            if (!set.next()) {
                return info;
            }

            info.add(new Pair<>("Number of Pages", set.getShort("pages")));
            info.add(new Pair<>("Number of Prices", set.getLong("prices")));
            info.add(new Pair<>("First Update", set.getTimestamp("first")));
            info.add(new Pair<>("Last Update", set.getTimestamp("last")));

            return info;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isConnected() {
        try (Connection con = source.getConnection()) {
            return con.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

}