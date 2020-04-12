package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.CommandInfo;
import me.lyphium.pagepriceparser.utils.Utils;

import java.awt.*;
import java.net.URL;

@CommandInfo(
        description = "Adds a new page to the database",
        usage = "addpage <name> <url> <address> [color]",
        aliases = "add"
)
public class AddPageCommand extends Command {

    public AddPageCommand() {
        super("addpage");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length < 3 || args.length > 4) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available, otherwise can't add page
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        // Check if url is valid
        try {
            new URL(args[1]).toURI();
        } catch (Exception e) {
            System.err.println("Not a valid url!");
            return true;
        }

        // Parse or create color for page
        final Color color;
        if (args.length == 3) {
            // No color given -> create random one
            color = Utils.randomColor();
        } else {
            // Color given -> parse color
            color = Utils.parseColor(args[3]);

            if (color == null) {
                System.err.println("Color must have the form: '#a12c6f' or '#a3b");
                return true;
            }
        }

        // Create page based on input
        final PriceData data = new PriceData(-1, args[0], args[1], args[2], color);

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