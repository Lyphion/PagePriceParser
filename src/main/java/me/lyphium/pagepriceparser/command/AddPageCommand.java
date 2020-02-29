package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Command;

import java.net.URL;

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

        // Checking if the connection to the database is available, otherwise can't add page
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        try {
            new URL(args[1]).toURI();
        } catch (Exception e) {
            System.err.println("No a valid url!");
            return true;
        }

        // Create page based on input
        final PriceData data = new PriceData(-1, args[0], args[1], args[2]);

        // Add page to database, success will be true, if the pages was added
        final boolean success = database.addPage(data);

        // Check if page was added
        if (success) {
            System.out.println("New page added to database");
        } else {
            System.err.println("Page couldn't added to database");
        }

        return true;
    }

}