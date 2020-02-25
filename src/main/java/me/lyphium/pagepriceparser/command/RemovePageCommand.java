package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Command;

public class RemovePageCommand extends Command {

    public RemovePageCommand() {
        super("removepage", "Removes a page and data from the database", "removepage <id> [name] or removepage <name>", new String[]{"remove"});
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length == 0 || args.length > 2) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available, otherwise can't remove page
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        final PriceData data;

        // Parse input, check if first argument is a number
        if (args[0].matches("(\\d)+")) {
            final int id = Integer.parseUnsignedInt(args[0]);
            final String name;

            //Check if second exists -> name
            if (args.length == 2) {
                name = args[1];
            } else {
                name = null;
            }

            // Create Temp-PriceData
            data = new PriceData(id, name, null, null);
        } else {
            final String name = args[0];

            // Create Temp-PriceData with name
            data = new PriceData(-1, name, null, null);
        }

        // Remove page and data to database, success will be true, if the pages was removed
        final boolean success = database.removePage(data);

        // Check if page and data was removed
        if (success) {
            System.out.println("Page and data removed from database");
        } else {
            System.err.println("Page couldn't removed from database");
        }

        return true;
    }

}
