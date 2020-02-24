package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;

public class UpdateCommand extends Command {

    public UpdateCommand() {
        super("update", "Updates the prices now", "update");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length > 0) {
            return false;
        }

        final Bot bot = Bot.getInstance();
        if (!bot.getDatabase().isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        long time = System.currentTimeMillis();
        System.out.println("Updating Prices...");

        bot.getParser().update();

        time = System.currentTimeMillis() - time;
        System.out.println("Finished: Updated the Prices (" + time + "ms)");

        return true;
    }

}