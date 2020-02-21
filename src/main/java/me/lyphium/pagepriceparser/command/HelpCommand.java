package me.lyphium.pagepriceparser.command;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Lists all commands", "help [Command]", new String[]{"?"});
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        final StringBuilder builder = new StringBuilder("-------- Help Page --------\n");

        if (args.length == 0) {
            for (Command command : getCommands()) {
                builder.append("» ");
                if (command.usage.isEmpty()) {
                    builder.append(command.name);
                } else {
                    builder.append(command.usage);
                }
                if (!command.description.isEmpty()) {
                    builder.append(" | ").append(command.description);
                }
                builder.append('\n');
            }
        } else if (args.length == 1) {
            final String cmdLabel = args[0];
            final Command command = getCommand(cmdLabel);

            if (command == null) {
                builder.append("Command '").append(cmdLabel).append("' not found!\n");
            } else {
                builder.append("» ").append("Command: ").append(command.name).append('\n');
                if (!command.description.isEmpty())
                    builder.append("» ").append("Description: ").append(command.description).append('\n');
                if (!command.usage.isEmpty())
                    builder.append("» ").append("Usage: ").append(command.usage).append('\n');
                if (command.getAliases().length > 0)
                    builder.append("» ").append("Aliases: ").append(String.join(", ", command.getAliases())).append('\n');
            }
        } else {
            return false;
        }
        System.out.print(builder.toString());

        return true;
    }

}