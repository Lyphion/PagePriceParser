package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;

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

        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        Timestamp begin = new Timestamp(0);
        Timestamp end = new Timestamp(System.currentTimeMillis());

        if (args.length > 2) {
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

        if (args[0].equalsIgnoreCase("id") || args[0].equalsIgnoreCase("name")) {
            final PriceData data;
            if (args[0].equalsIgnoreCase("id")) {
                if (args[1].matches("(\\d){1,4}")) {
                    final int id = Integer.parseUnsignedInt(args[1]);
                    data = database.getPriceData(id, begin, end);
                } else {
                    System.err.println("Invalid id format or to many digits");
                    return true;
                }
            } else {
                final String name = args[0];
                data = database.getPriceData(name, begin, end);
            }

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

            final Map<Fuel, Map<Long, Float>> prices = data.getPrices();

            long min = System.currentTimeMillis(), max = 0;
            for (Map<Long, Float> map : prices.values()) {
                for (long l : map.keySet()) {
                    if (l < min) {
                        min = l;
                    }
                    if (l > max) {
                        max = l;
                    }
                }
            }
            begin.setTime(min);
            end.setTime(max);

            final StringBuilder builder = new StringBuilder("--------- Page Information ---------\n");
            builder.append(String.format("ID:      %d\n", data.getId()));
            builder.append(String.format("Name:    %s\n", data.getName()));
            builder.append(String.format("URL:     %s\n", data.getUrl()));
            builder.append(String.format("Address: %s\n", data.getAddress()));

            if (max == 0) {
                builder.append("\nNo price data available\n");
                System.out.print(builder.toString());
                return true;
            }

            builder.append(String.format("Begin:   %s\n", toString(begin)));
            builder.append(String.format("End:     %s\n\n", toString(end)));

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

            System.out.print(builder.toString());
        } else if (args[0].equalsIgnoreCase("fuel")) {
            final Fuel fuel = Fuel.valueOf(args[1]);
            if (fuel == null) {
                System.err.println("Invalid fuel");
                return true;
            }

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

            // TODO Print table, split into groups -> table to many columns
        } else if (args[0].equalsIgnoreCase("pages")) {
            if (args.length > 2) {
                return false;
            }

            final List<PriceData> pages = database.getPages();

            if (args.length == 2) {
                final String pattern = args[1];
                pages.removeIf(s -> !s.getName().matches(pattern));
            }

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

            for (PriceData page : pages) {
                builder.append('\n');
                builder.append(String.format("ID:      %d\n", page.getId()));
                builder.append(String.format("Name:    %s\n", page.getName()));
                builder.append(String.format("URL:     %s\n", page.getUrl()));
                builder.append(String.format("Address: %s\n", page.getAddress()));
            }

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
