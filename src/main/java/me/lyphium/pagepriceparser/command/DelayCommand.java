package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.Utils;

public class DelayCommand extends Command {

    public DelayCommand() {
        super("delay", "Get or change update delay", "delay [value]");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length > 1) {
            return false;
        }

        final Bot bot = Bot.getInstance();

        // Check if only request or delay change
        if (args.length == 0) {
            // Get current delay
            final long delay = bot.getParser().getDelay();
            System.out.println("Current delay: " + delay + "ms");
        } else {
            final long delay;

            // Parse new delay as a number or time string
            if (args[0].matches("(\\d)+")) {
                delay = Long.parseUnsignedLong(args[0]);
            } else {
                delay = Utils.calculateDelay(args[0]);
            }

            // Set new delay
            bot.getParser().setDelay(delay);
            System.out.println("New delay: " + delay + "ms");
        }

        return true;
    }

}
