package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Command;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrintCommand extends Command {

    public PrintCommand() {
        super("print", "Print information from the database", "print <id/name/fuel> <value> [begin] [end] or print pages [pattern]");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length == 0 || args.length > 4 || (args.length == 1 && !args[0].equalsIgnoreCase("pages"))) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available, otherwise can't print Informations
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        Timestamp begin = new Timestamp(0);
        Timestamp end = new Timestamp(System.currentTimeMillis());

        // Parse begin and end time
        if (args.length > 2) {
            // Parse begin time as a number or time string
            if (args[2].matches("(\\d)+")) {
                begin = new Timestamp(Long.parseUnsignedLong(args[2]));
            } else {
                try {
                    begin = Timestamp.valueOf(args[2]);
                } catch (Exception e) {
                    System.err.println("Invalid time format: 'milliseconds' or 'yyyy-[m]m-[d]d hh:mm:ss'");
                    return true;
                }
            }

            if (args.length > 3) {
                // Parse end time as a number or time string
                if (args[3].matches("(\\d)+")) {
                    end = new Timestamp(Long.parseUnsignedLong(args[3]));
                } else {
                    try {
                        end = Timestamp.valueOf(args[2]);
                    } catch (Exception e) {
                        System.err.println("Invalid time format: 'milliseconds' or 'yyyy-[m]m-[d]d hh:mm:ss'");
                        return true;
                    }
                }
            }
        }

        // Handle each cathegory differently
        if (args[0].equalsIgnoreCase("id") || args[0].equalsIgnoreCase("name")) {
            final PriceData data;

            // Get PriceData based on the input id or name
            if (args[0].equalsIgnoreCase("id")) {
                if (args[1].matches("(\\d){1,4}")) {
                    // Parse id
                    final int id = Integer.parseUnsignedInt(args[1]);

                    // Get PriceData from database
                    data = database.getPriceData(id, begin, end);
                } else {
                    System.err.println("Invalid id format or to many digits");
                    return true;
                }
            } else {
                // Parse name
                final String name = args[0];

                // Get PriceData from database
                data = database.getPriceData(name, begin, end);
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
             *  Begin:   -begin-
             *  End:     -end-
             *
             *   Time |  Fuel1 |  Fuel2 | ...
             *  ------+--------+--------+------
             *        | 1.259€ | 1.359€ |
             *        |  ....  |  ....  |
             *        |        |        |
             */

            // Price Informations by Fuel and Time
            final Map<Fuel, Map<Long, Float>> prices = data.getPrices();

            // Calculation the first and last Price Entry
            long min = System.currentTimeMillis(), max = 0;
            boolean changed = false;
            for (Map<Long, Float> map : prices.values()) {
                for (long l : map.keySet()) {
                    if (l < min) {
                        min = l;
                        changed = true;
                    }
                    if (l > max) {
                        max = l;
                        changed = true;
                    }
                }
            }
            if (changed) {
                begin.setTime(min);
                end.setTime(max);
            }

            // Building the head of the Information page
            final StringBuilder builder = new StringBuilder("--------- Page Information ---------\n");
            builder.append(String.format("ID:      %d\n", data.getId()));
            builder.append(String.format("Name:    %s\n", data.getName()));
            builder.append(String.format("URL:     %s\n", data.getUrl()));
            builder.append(String.format("Address: %s\n", data.getAddress()));

            builder.append(String.format("Begin:   %s\n", toString(begin)));
            builder.append(String.format("End:     %s\n\n", toString(end)));

            // Check if PriceData contains Price Information
            if (!changed) {
                builder.append("No price data available\n");
                System.out.print(builder.toString());
                return true;
            }

            final List<Fuel> fuels = new ArrayList<>(prices.keySet());
            final int[] colSize = new int[fuels.size()];

            // Calculate column width
            for (int i = 0; i < fuels.size(); i++) {
                final Fuel fuel = fuels.get(i);
                colSize[i] = Math.max(7, fuel.toString().length());
            }

            // Table head
            builder.append("                Time");
            for (int i = 0; i < fuels.size(); i++) {
                final Fuel fuel = fuels.get(i);
                builder.append(" | ").append(String.format("%" + colSize[i] + "s", fuel.toString()));
            }
            builder.append("\n");

            // Table separator
            builder.append(new String(new char[21]).replace('\0', '-'));
            for (int i = 0; i < fuels.size(); i++) {
                builder.append("+").append(new String(new char[colSize[i] + 2]).replace('\0', '-'));
            }
            builder.append('\n');

            // Collecting all available time entries
            final Timestamp time = new Timestamp(0);
            final List<Long> times = prices.values().parallelStream().flatMap(m -> m.keySet().stream()).distinct().sorted().collect(Collectors.toList());

            // Table body
            for (long l : times) {
                time.setTime(l);
                builder.append(String.format(" %s", toString(time)));

                for (int j = 0; j < fuels.size(); j++) {
                    final Fuel fuel = fuels.get(j);
                    if (data.hasPrice(fuel, l)) {
                        builder.append(String.format(" |%" + colSize[j] + ".3f€", data.getPrice(fuel, l)));
                    } else {
                        builder.append(String.format(" |%" + colSize[j] + "s ", ""));
                    }
                }
                builder.append('\n');
            }

            // Printing Information page
            System.out.print(builder.toString());
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

            // Get List of PriceData based on the fuel
            final List<PriceData> data = database.getPriceData(fuel, begin, end);

            /*
             *  Fuel:  -fuel-
             *  Begin: -begin-
             *  End:   -end-
             *
             *   Time |  Page1 |  Page2 | ...
             *  ------+--------+--------+------
             *        | 1.259€ | 1.359€ |
             *        |  ....  |  ....  |
             *        |        |        |
             */

            // Calculation the first and last Price Entry
            long min = System.currentTimeMillis(), max = 0;
            boolean changed = false;
            for (PriceData priceData : data) {
                for (Long l : priceData.getPrices(fuel).keySet()) {
                    if (l < min) {
                        min = l;
                        changed = true;
                    }
                    if (l > max) {
                        max = l;
                        changed = true;
                    }
                }
            }
            if (changed) {
                begin.setTime(min);
                end.setTime(max);
            }

            // Building the head of the Information page
            final StringBuilder builder = new StringBuilder("--------- Fuel Information ---------\n");
            builder.append(String.format("Fuel:  %s\n", fuel.getName()));
            builder.append(String.format("Begin: %s\n", toString(begin)));
            builder.append(String.format("End:   %s\n\n", toString(end)));

            // Check if List of PriceData contains Price Information
            if (!changed) {
                builder.append("No price data available\n");
                System.out.print(builder.toString());
                return true;
            }

            final int[] colSize = new int[data.size()];

            // Calculate column width
            for (int i = 0; i < data.size(); i++) {
                final PriceData priceData = data.get(i);
                colSize[i] = Math.max(7, priceData.getName().length());
            }

            // Table head
            builder.append("                Time");
            for (int i = 0; i < data.size(); i++) {
                final PriceData priceData = data.get(i);
                builder.append(" | ").append(String.format("%" + colSize[i] + "s", priceData.getName()));
            }
            builder.append("\n");

            // Table separator
            builder.append(new String(new char[21]).replace('\0', '-'));
            for (int i = 0; i < data.size(); i++) {
                builder.append("+").append(new String(new char[colSize[i] + 2]).replace('\0', '-'));
            }
            builder.append('\n');

            // Collecting all available time entries
            final Timestamp time = new Timestamp(0);
            final List<Long> times = data.parallelStream().flatMap(m -> m.getPrices(fuel).keySet().stream()).distinct().sorted().collect(Collectors.toList());

            // Table body
            for (long l : times) {
                time.setTime(l);
                builder.append(String.format(" %s", toString(time)));

                for (int j = 0; j < data.size(); j++) {
                    final PriceData priceData = data.get(j);
                    if (priceData.hasPrice(fuel, l)) {
                        builder.append(String.format(" |%" + colSize[j] + ".3f€", priceData.getPrice(fuel, l)));
                    } else {
                        builder.append(String.format(" |%" + colSize[j] + "s ", ""));
                    }
                }
                builder.append('\n');
            }

            // Printing Information page
            System.out.print(builder.toString());
        } else if (args[0].equalsIgnoreCase("pages")) {
            if (args.length > 2) {
                return false;
            }

            // Getting all pages from database
            final List<PriceData> pages = database.getPages();

            // Filtering all pages with don't match the optimal pattern
            if (args.length == 2) {
                final String pattern = args[1];
                pages.removeIf(s -> !s.getName().matches(pattern));
            }

            // Check if there are still some pages
            if (pages.isEmpty()) {
                System.out.println("No pages found");
                return true;
            }

            /*
             *  ID:      -id-
             *  Name:    -name-
             *  URL:     -url-
             *  Address: -address-
             *
             *  ID:      -id-
             *  Name:    -name-
             *  ...
             */

            final StringBuilder builder = new StringBuilder("---------- Available Pages ----------");

            // Creating the Information section for each page
            for (PriceData page : pages) {
                builder.append('\n');
                builder.append(String.format("ID:      %d\n", page.getId()));
                builder.append(String.format("Name:    %s\n", page.getName()));
                builder.append(String.format("URL:     %s\n", page.getUrl()));
                builder.append(String.format("Address: %s\n", page.getAddress()));
            }

            // Printing Information page
            System.out.print(builder.toString());
        } else {
            return false;
        }

        return true;
    }

    private String toString(Timestamp time) {
        final String s = time.toString();
        final int index = s.lastIndexOf('.');

        return index == -1 ? s : s.substring(0, index);
    }

}
