package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.CommandInfo;

@CommandInfo(
        description = "Updates the prices now",
        usage = "update"
)
public class UpdateCommand extends Command {

    public UpdateCommand() {
        super("update");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length > 0) {
            return false;
        }

        final Bot bot = Bot.getInstance();

        // Checking if the connection to the database is available, otherwise can't update
        if (!bot.getDatabase().isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        long time = System.currentTimeMillis();
        System.out.println("Updating Prices...");

        // Update the pages
        bot.getParser().update();

        time = System.currentTimeMillis() - time;
        System.out.println("Finished: Updated the Prices (" + time + "ms)");

        return true;
    }

}