package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.CommandInfo;

@CommandInfo(
        description = "Get delay time",
        usage = "delay"
)
public class DelayCommand extends Command {

    public DelayCommand() {
        super("delay");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length > 0) {
            return false;
        }

        final Bot bot = Bot.getInstance();

        // Get current delay
        long delay = bot.getParser().getDelay();

        if (delay < 0) {
            System.out.println("Page parser is disabled");
        } else if (delay < 1000) {
            System.out.println("Current delay: " + delay + "ms");
        } else {
            System.out.println("Current delay: " + (delay / 1000) + "s");
        }

        return true;
    }

}
