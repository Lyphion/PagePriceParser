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

        long time = System.currentTimeMillis();
        System.out.println("Updating Prices...");

        Bot.getInstance().getParser().update();

        time = System.currentTimeMillis() - time;
        System.out.println("Finished: Updated the Prices (" + time + "ms)");

        return true;
    }

}