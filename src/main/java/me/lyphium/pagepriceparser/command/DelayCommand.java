package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.CommandInfo;
import me.lyphium.pagepriceparser.utils.Utils;

@CommandInfo(
        description = "Get or change update delay",
        usage = "delay [value]"
)
public class DelayCommand extends Command {

    public DelayCommand() {
        super("delay");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length > 1) {
            return false;
        }

        final Bot bot = Bot.getInstance();

        // Get current delay
        long delay = bot.getParser().getDelay();

        // Check if only request or delay change
        if (args.length == 0) {
            if (delay < 0) {
                System.out.println("Page parser is disabled");
            } else {
                System.out.println("Current delay: " + delay + "ms");
            }
        } else {
            if (delay < 0) {
                System.out.println("Page parser is disabled. To change delay restart Bot");
                return true;
            }

            // Parse new delay as a number or time string
            delay = Utils.calculateDelay(args[0]);

            // Set new delay
            bot.getParser().setDelay(delay);

            if (delay < 0) {
                bot.getParser().cancel();
                System.out.println("Shut down Page Parser");
            } else {
                System.out.println("New delay: " + delay + "ms");
            }
        }

        return true;
    }

}
