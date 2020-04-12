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

        Bot.getInstance().getParser().update();

        return true;
    }

}