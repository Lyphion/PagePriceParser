package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Command;

public class AddPageCommand extends Command {

    public AddPageCommand() {
        super("addpage", "Adds a new page to the database", "addpage <name> <url> <address>", new String[]{"add"});
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length != 3) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        final PriceData data = new PriceData(-1, args[0], args[1], args[2]);
        final boolean success = database.addPage(data);

        if (success) {
            System.out.println("New page added to database");
        } else {
            System.err.println("Page couldn't added to database");
        }

        return true;
    }

}