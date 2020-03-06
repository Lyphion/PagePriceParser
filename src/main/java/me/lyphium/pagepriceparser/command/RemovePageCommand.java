package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Command;

import java.sql.Timestamp;

public class RemovePageCommand extends Command {

    private long removeTime = 0;
    private PriceData toBeRemoved = null;

    public RemovePageCommand() {
        super("removepage", "Removes a page and data from the database", "removepage <id/name> [value]", new String[]{"remove"});
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length == 0 || args.length == 1 && !args[0].equalsIgnoreCase("confirm") || args.length > 2) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available, otherwise can't remove page
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        // Parse input, check if first argument is a number
        if (args[0].equalsIgnoreCase("id")) {
            final int id = Integer.parseUnsignedInt(args[1]);

            // Create Temp-PriceData
            final PriceData data = database.getPriceData(id, new Timestamp(0), new Timestamp(0));

            // Check if page exists
            if (data == null) {
                System.err.println("No data found with id: " + id);
                return true;
            }

            // Create Delete Confirm
            System.out.println("Do you really want to delete '" + data.getName() + "'? Confirm with: 'removepage confirm'");

            toBeRemoved = data;
            removeTime = System.currentTimeMillis();
        } else if (args[0].equalsIgnoreCase("name")) {
            final String name = args[1];

            // Create Temp-PriceData with name
            final PriceData data = database.getPriceData(name, new Timestamp(0), new Timestamp(0));

            // Check if page exists
            if (data == null) {
                System.err.println("No data found with name: " + name);
                return true;
            }

            // Create Delete Confirm
            System.out.println("Do you really want to delete '" + data.getName() + "'? Confirm with: 'removepage confirm'");

            toBeRemoved = data;
            removeTime = System.currentTimeMillis();
        } else if (args[0].equalsIgnoreCase("confirm")) {
            // Check if samething should be removed
            if (toBeRemoved == null) {
                System.err.println("Nothing to remove");
                return true;
            }

            // Check if Confirm Time is smaller than 10sec
            if (removeTime + 15 * 1000 < System.currentTimeMillis()) {
                System.err.println("Confirm time expired");
                toBeRemoved = null;
                return true;
            }

            // Remove page and data to database, success will be true, if the pages was removed
            final boolean success = database.removePage(toBeRemoved);
            toBeRemoved = null;

            // Check if page and data was removed
            if (success) {
                System.out.println("Page and data removed from database");
            } else {
                System.err.println("Page couldn't removed from database");
            }
        } else {
            return false;
        }

        return true;
    }

}
