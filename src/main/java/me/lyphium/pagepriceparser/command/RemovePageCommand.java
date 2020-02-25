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

        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        final PriceData data;
        if (args[0].matches("(\\d)+")) {
            final int id = Integer.parseUnsignedInt(args[0]);
            final String name;
            if (args.length == 2) {
                name = args[1];
            } else {
                name = null;
            }
            data = new PriceData(id, name, null, null);
        } else {
            final String name = args[0];
            data = new PriceData(-1, name, null, null);
        }

        final boolean success = database.removePage(data);

        if (success) {
            System.out.println("Page and data removed from database");
        } else {
            System.err.println("Page couldn't removed from database");
        }

        return true;
    }

}
