package me.lyphium.pagepriceparser.command;

import java.util.*;

public abstract class Command {

    private static final List<Command> COMMANDS = new ArrayList<>();

    protected final String name;
    protected final String description;
    protected final String usage;
    private final String[] aliases;

    public Command(String name, String description, String usage, String[] aliases) {
        this.name = name.trim().toLowerCase();
        this.description = description.trim();
        this.usage = usage.trim();

        final List<String> list = new ArrayList<>();
        for (String alias : aliases) {
            if (alias != null) {
                list.add(alias.trim().toLowerCase());
            }
        }
        this.aliases = list.toArray(new String[0]);
    }

    public Command(String name, String description, String usage) {
        this.name = name.trim().toLowerCase();
        this.description = description.trim();
        this.usage = usage.trim();
        this.aliases = new String[0];
    }

    public abstract boolean onCommand(String label, String[] args);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public final String[] getAliases() {
        return Arrays.copyOf(aliases, aliases.length);
    }

    public static boolean registerCommand(Command newCommand) {
        for (Command command : COMMANDS) {
            if (command.name.equals(newCommand.name)) {
                return false;
            }
        }

        COMMANDS.add(newCommand);
        COMMANDS.sort(Comparator.comparing(c -> c.name));

        return true;
    }

    public static boolean unregisterCommand(Command command) {
        return COMMANDS.remove(command);
    }

    public static Command getCommand(String name) {
        final String label = name.trim().toLowerCase();
        for (Command command : COMMANDS) {
            if (command.name.equals(label)) {
                return command;
            }
        }
        for (Command command : COMMANDS) {
            for (String alias : command.aliases) {
                if (alias.equals(label)) {
                    return command;
                }
            }
        }
        return null;
    }

    public static List<Command> getCommands() {
        return Collections.unmodifiableList(COMMANDS);
    }

    public static boolean execute(String commandString) {
        final String[] rawSplit = commandString.trim().split(" ", 2);
        final String label = rawSplit[0];
        final Command command = getCommand(label);

        if (command == null) {
            return false;
        }

        try {
            boolean success;
            if (rawSplit.length == 1) {
                success = command.onCommand(label, new String[0]);
            } else {
                final String[] args = rawSplit[1].split(" ");
                success = command.onCommand(label, args);
            }
            if (!success && !command.usage.isEmpty()) {
                System.out.println("Usage: " + command.usage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

}