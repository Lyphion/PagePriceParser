package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@CommandInfo(
        description = "Provide information about database and prices",
        shortUsage = "info <id/name/fuel> <value>",
        usage = "info or info <id/name/fuel> <value> [time] [pattern]"
)
public class InfoCommand extends Command {

    public InfoCommand() {
        super("info");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length == 1 || args.length > 4) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available, otherwise can't add page
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        // General inforation about the database such as amount of entries
        if (args.length == 0) {
            // Get information from database
            final List<Pair<String, Object>> info = database.getDatabaseInformation();

            // Calculate offset to format key/value pairs
            final int offset = info.stream().mapToInt(v -> v.getFirst().length()).max().orElse(0) + 1;

            final StringBuilder builder = new StringBuilder("--------- Information Page ---------\n");

            // Create information lines
            for (Pair<String, Object> pair : info) {
                builder.append(String.format("%-" + offset + "s ", pair.getFirst() + ":"));
                if (pair.getSecond() instanceof Date) {
                    builder.append(Utils.toString((Date) pair.getSecond()));
                } else {
                    builder.append(pair.getSecond());
                }
                builder.append('\n');
            }

            System.out.print(builder.toString());

            return true;
        }

        // Parse time
        final Timestamp timestamp;
        if (args.length > 2) {
            timestamp = Utils.toTimestamp(args[2]);

            if (timestamp == null) {
                System.err.println("Invalid time format: 'milliseconds' or 'yyyy-[m]m-[d]d hh:mm:ss'");
                return true;
            }
        } else {
            timestamp = new Timestamp(System.currentTimeMillis());
        }
        final long time = timestamp.getTime();

        if (args[0].equalsIgnoreCase("id") || args[0].equalsIgnoreCase("name")) {
            final PriceData data;

            // Get PriceData based on the input id or name
            if (args[0].equalsIgnoreCase("id")) {
                if (args[1].matches("(\\d){1,4}")) {
                    // Parse id
                    final int id = Integer.parseUnsignedInt(args[1]);

                    // Get PriceData from database
                    data = database.getPriceData(id, new Timestamp(0), new Timestamp(System.currentTimeMillis()));
                } else {
                    System.err.println("Invalid id format or to many digits");
                    return true;
                }
            } else {
                // Parse name
                final String name = args[1];

                // Get PriceData from database
                data = database.getMostSimilarPriceData(name, new Timestamp(0), new Timestamp(System.currentTimeMillis()));
            }

            // Check if data was found
            if (data == null) {
                System.err.println("No price data found");
                return true;
            }

            /*
             *  ID:      -id-
             *  Name:    -name-
             *  URL:     -url-
             *  Address: -address-
             *
             *        |  Fuel1 |  Fuel2 | ...
             *  ------+--------+--------+------
             *    Min | 1.259€ | 1.359€ |
             *   Time |  ....  |  ....  |
             *    Max |        |        |
             */

            final StringBuilder builder = new StringBuilder("--------- Information Page ---------\n");
            builder.append(String.format("ID:      %d\n", data.getId()));
            builder.append(String.format("Name:    %s\n", data.getName()));
            builder.append(String.format("URL:     %s\n", data.getUrl()));
            builder.append(String.format("Address: %s\n\n", data.getAddress()));

            final Map<Fuel, PriceMap> prices = data.getPrices();

            // Apply pattern if exists
            if (args.length > 3) {
                final String pattern = args[3];
                prices.keySet().removeIf(f -> !f.getName().matches(pattern));
            }

            // Check if List of PriceData contains Price Information
            if (prices.size() == 0 || prices.values().stream().map(PriceMap::size).count() == 0) {
                builder.append("No price data available");
                System.out.println(builder.toString());
                return true;
            }

            final List<Fuel> fuels = new ArrayList<>(prices.keySet());
            final int[] colSize = new int[fuels.size()];

            // Calculate column width
            for (int i = 0; i < fuels.size(); i++) {
                final Fuel fuel = fuels.get(i);
                colSize[i] = Math.max(7, fuel.getName().length());
            }

            // Table head
            builder.append("                    ");
            for (int i = 0; i < fuels.size(); i++) {
                final Fuel fuel = fuels.get(i);
                builder.append(" | ").append(String.format("%" + colSize[i] + "s", fuel.getName()));
            }
            builder.append('\n');

            // Table separator
            builder.append(new String(new char[21]).replace('\0', '-'));
            for (int i = 0; i < fuels.size(); i++) {
                builder.append("+").append(new String(new char[colSize[i] + 2]).replace('\0', '-'));
            }
            builder.append("\n");

            // Table body
            // Minimum Price
            builder.append("       Minimum Price");

            for (int j = 0; j < fuels.size(); j++) {
                final Fuel fuel = fuels.get(j);

                float min = Float.MAX_VALUE;
                for (float v : prices.get(fuel).values()) {
                    if (v < min) {
                        min = v;
                    }
                }

                builder.append(String.format(" |%" + colSize[j] + ".3f€", min));
            }

            // Price of time
            builder.append(String.format("\n %s", Utils.toString(timestamp)));

            for (int j = 0; j < fuels.size(); j++) {
                final Fuel fuel = fuels.get(j);

                final int index = prices.get(fuel).nearestIndexOf(time) - 1;
                if (index >= 0) {
                    final float price = prices.get(fuel).get(index);

                    builder.append(String.format(" |%" + colSize[j] + ".3f€", price));
                } else {
                    builder.append(String.format(" | %" + colSize[j] + "s", "Unknown"));
                }
            }

            // Maximum Price
            builder.append("\n       Maximum Price");

            for (int j = 0; j < fuels.size(); j++) {
                final Fuel fuel = fuels.get(j);

                float max = Float.MIN_VALUE;
                for (float v : prices.get(fuel).values()) {
                    if (v > max) {
                        max = v;
                    }
                }

                builder.append(String.format(" |%" + colSize[j] + ".3f€", max));
            }

            System.out.println(builder.toString());
        } else if (args[0].equalsIgnoreCase("fuel")) {
            final Fuel fuel;

            // Parsing fuel as an id or name
            if (args[1].matches("(\\d)+")) {
                fuel = Fuel.getById(Integer.parseUnsignedInt(args[1]));
            } else {
                fuel = Fuel.getByName(args[1]);
            }

            // Check if input is valid
            if (fuel == null) {
                System.err.println("Invalid fuel");
                return true;
            }

            /*
             *  Fuel:  -fuel-
             *
             *   Time |  Page1 |  Page2 | ...
             *  ------+--------+--------+------
             *    Min | 1.259€ | 1.359€ |
             *   Time |  ....  |  ....  |
             *    Max |        |        |
             */

            final List<PriceData> data = database.getPriceData(fuel, new Timestamp(0), new Timestamp(System.currentTimeMillis()));

            // Apply pattern if exists
            if (args.length > 3) {
                final String pattern = args[3];
                data.removeIf(s -> !s.getName().matches(pattern));
            }

            // Building the head of the Information page
            final StringBuilder builder = new StringBuilder("--------- Information Page ---------\n");
            builder.append(String.format("Fuel:  %s\n\n", fuel.getName()));

            // Check if List of PriceData contains Price Information
            if (data.isEmpty() || data.stream().map(p -> p.getPrices(fuel).size()).count() == 0) {
                builder.append("No price data available");
                System.out.println(builder.toString());
                return true;
            }

            final int[] colSize = new int[data.size()];
            final String[][] headLines = new String[data.size()][];
            int maxHeadLines = 1;

            // Calculate column width
            for (int i = 0; i < data.size(); i++) {
                final PriceData priceData = data.get(i);
                headLines[i] = Utils.wordWrap(priceData.getName(), 15);

                int len = 7;
                for (String line : headLines[i]) {
                    if (line.length() > len) {
                        len = line.length();
                    }
                }

                if (headLines[i].length > maxHeadLines) {
                    maxHeadLines = headLines[i].length;
                }

                colSize[i] = len;
            }

            // Table head
            for (int i = 0; i < maxHeadLines; i++) {
                builder.append(String.format("%20s", ""));
                for (int j = 0; j < data.size(); j++) {
                    final int dLines = maxHeadLines - headLines[j].length;

                    builder.append(" | ").append(String.format(
                            "%" + colSize[j] + "s", i >= dLines ? headLines[j][i - dLines] : "")
                    );
                }
                builder.append('\n');
            }

            // Table separator
            builder.append(new String(new char[21]).replace('\0', '-'));
            for (int i = 0; i < data.size(); i++) {
                builder.append("+").append(new String(new char[colSize[i] + 2]).replace('\0', '-'));
            }

            // Table body
            // Minimum Price
            builder.append("\n       Minimum Price");

            for (int j = 0; j < data.size(); j++) {
                final PriceData priceData = data.get(j);

                float min = Float.MAX_VALUE;
                for (float v : priceData.getPrices(fuel).values()) {
                    if (v < min) {
                        min = v;
                    }
                }

                builder.append(String.format(" |%" + colSize[j] + ".3f€", min));
            }

            // Price of time
            builder.append(String.format("\n %s", Utils.toString(timestamp)));

            for (int j = 0; j < data.size(); j++) {
                final PriceData priceData = data.get(j);

                final int index = priceData.getPrices(fuel).nearestIndexOf(time) - 1;
                if (index >= 0) {
                    final float price = priceData.getPrices(fuel).get(index);

                    builder.append(String.format(" |%" + colSize[j] + ".3f€", price));
                } else {
                    builder.append(String.format(" | %" + colSize[j] + "s", "Unknown"));
                }
            }

            // Maximum Price
            builder.append("\n       Maximum Price");

            for (int j = 0; j < data.size(); j++) {
                final PriceData priceData = data.get(j);

                float max = Float.MIN_VALUE;
                for (float v : priceData.getPrices(fuel).values()) {
                    if (v > max) {
                        max = v;
                    }
                }

                builder.append(String.format(" |%" + colSize[j] + ".3f€", max));
            }

            System.out.println(builder.toString());
        } else {
            return false;
        }

        return true;
    }

}
