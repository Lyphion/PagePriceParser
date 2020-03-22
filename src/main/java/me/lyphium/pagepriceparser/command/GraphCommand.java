package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.GraphImage;
import me.lyphium.pagepriceparser.utils.PriceMap;
import me.lyphium.pagepriceparser.utils.Utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GraphCommand extends Command {

    public GraphCommand() {
        super("graph", "Create a graph of the prices", "graph <id/name/fuel> <value> <file> [begin] [end]");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length < 3 || args.length > 5) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available, otherwise can't print Informations
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        final File file = new File(args[2]);
        try {
            // Check if path is valid
            file.getCanonicalPath();
        } catch (IOException e) {
            System.err.println("Invalid file path");
            return true;
        }

        Timestamp begin = new Timestamp(0);
        Timestamp end = new Timestamp(System.currentTimeMillis());

        // Parse begin and end time
        if (args.length > 3) {
            begin = Utils.toTimestamp(args[3]);

            if (begin == null) {
                System.err.println("Invalid time format: 'milliseconds' or 'yyyy-[m]m-[d]d hh:mm:ss'");
                return true;
            }

            if (args.length > 4) {
                end = Utils.toTimestamp(args[4]);

                if (begin == null) {
                    System.err.println("Invalid time format: 'milliseconds' or 'yyyy-[m]m-[d]d hh:mm:ss'");
                    return true;
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
                final String name = args[1];

                // Get PriceData from database
                data = database.getMostSimilarPriceData(name, begin, end);
            }

            // Check if data was found
            if (data == null) {
                System.err.println("No price data found");
                return true;
            }

            // Price Informations by Fuel and Time
            final Map<Fuel, PriceMap> prices = data.getPrices();

            // Map values
            final Map<String, PriceMap> map = new HashMap<>();
            final Map<String, Color> colors = new HashMap<>();
            for (Entry<Fuel, PriceMap> entry : prices.entrySet()) {
                map.put(entry.getKey().getName(), entry.getValue());
                colors.put(entry.getKey().getName(), entry.getKey().getColor());
            }

            // Create an export image
            final GraphImage image = new GraphImage(data.getName(), map, colors, file);
            image.draw();
            image.export();
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

            // Map values
            final Map<String, PriceMap> map = new HashMap<>();
            final Map<String, Color> colors = new HashMap<>();
            for (PriceData priceData : data) {
                map.put(priceData.getName(), priceData.getPrices(fuel));
                colors.put(priceData.getName(), priceData.getColor());
            }

            // Create an export image
            final GraphImage image = new GraphImage(fuel.getName(), map, colors, file);
            image.draw();
            image.export();
            image.free();
        } else {
            return false;
        }

        System.gc();
        return true;
    }

}