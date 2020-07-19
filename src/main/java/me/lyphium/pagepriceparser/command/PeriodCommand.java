package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.CommandInfo;

@CommandInfo(
        description = "Get page check period time",
        usage = "period"
)
public class PeriodCommand extends Command {

    public PeriodCommand() {
        super("period");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length > 0) {
            return false;
        }

        final Bot bot = Bot.getInstance();

        // Get current period
        final long period = bot.getParser().getPeriod();

        if (period < 0) {
            System.out.println("Page parser is disabled");
        } else if (period < 1000) {
            System.out.println("Checking Pages every " + period + "ms");
        } else {
            System.out.println("Checking Pages every " + (period / 1000) + "sec");
        }

        return true;
    }

}
