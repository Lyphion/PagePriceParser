package me.lyphium.pagepriceparser.utils;

import lombok.Getter;

import java.util.*;

@Getter
public abstract class Command {

    private static final List<Command> COMMANDS = new ArrayList<>();

    private final String name;
    private final String description;
    private final String usage;
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
        list.sort(String::compareToIgnoreCase);

        this.aliases = list.toArray(new String[0]);
    }

    public Command(String name, String description, String usage) {
        this.name = name.trim().toLowerCase();
        this.description = description.trim();
        this.usage = usage.trim();
        this.aliases = new String[0];
    }

    public abstract boolean onCommand(String label, String[] args);

    public final String[] getAliases() {
        // Return copy of all aliases -> no modification possible
        return Arrays.copyOf(aliases, aliases.length);
    }

    public static boolean registerCommand(Command newCommand) {
        // Check if a command with this name already exists
        for (Command command : COMMANDS) {
            if (command.name.equals(newCommand.name)) {
                return false;
            }
        }

        // Add new command to list and sort list -> useful for help etc.
        COMMANDS.add(newCommand);
        COMMANDS.sort(Comparator.comparing(c -> c.name));

        return true;
    }

    public static boolean unregisterCommand(Command command) {
        // Remove command from list
        return COMMANDS.remove(command);
    }

    public static Command getCommand(String name) {
        final String label = name.trim().toLowerCase();

        // Check first for command names
        for (Command command : COMMANDS) {
            if (command.name.equals(label)) {
                return command;
            }
        }

        // Check Second for command aliases
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
        // Return unmodifiable copy of all command
        return Collections.unmodifiableList(COMMANDS);
    }

    public static boolean execute(String commandString) {
        final String[] rawSplit = commandString.trim().split(" ", 2);
        final String label = rawSplit[0];
        final Command command = getCommand(label);

        // Check if a command with this label exists
        if (command == null) {
            return false;
        }

        try {
            boolean success;
            // Check for argument length
            if (rawSplit.length == 1) {
                // Call command with no arguments
                success = command.onCommand(label, new String[0]);
            } else {
                final List<String> tokens = new ArrayList<>();
                final StringBuilder builder = new StringBuilder();

                boolean quote = false;
                for (char c : rawSplit[1].toCharArray()) {
                    // Check if quoted
                    if (c == '"') {
                        quote = !quote;
                        continue;
                    }

                    // Add part to list if space and not quoted
                    if (c == ' ' && !quote) {
                        // Check if something is in the builder
                        if (builder.length() > 0) {
                            tokens.add(builder.toString());
                            builder.setLength(0);
                        }
                    } else {
                        // Add char to builder
                        builder.append(c);
                    }
                }
                // Add last part to list
                tokens.add(builder.toString());

                // Call command with argument list
                success = command.onCommand(label, tokens.toArray(new String[0]));
            }

            // Check if command executed successfully, otherwise print usage message
            if (!success && !command.usage.isEmpty()) {
                System.out.println("Usage: " + command.usage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

}