package me.lyphium.pagepriceparser;

import lombok.Getter;
import me.lyphium.pagepriceparser.command.*;
import me.lyphium.pagepriceparser.connection.ConnectionManager;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.PageParser;
import me.lyphium.pagepriceparser.utils.Command;
import me.lyphium.pagepriceparser.utils.Utils;

import java.util.Scanner;

@Getter
public class Bot {

    @Getter
    private static Bot instance;

    private boolean running;
    private PageParser parser;
    private DatabaseConnection database;
    private ConnectionManager connectionManager;

    public Bot() {
        instance = this;
    }

    public void init(String[] args) {
        // Register all available commands
        registerCommands();

        // Parsing start arguments
        long delay = 60 * 60 * 1000;
        int port = 14703;
        for (int i = 0; i < args.length; i++) {
            final String part = args[i];

            // Parsing the delay time between checks
            if (part.equals("-d") && i < args.length - 1) {
                delay = Utils.calculateDelay(args[i + 1]);
            }
            // Parsing the Client Connection port
            else if (part.equals("-p") && i < args.length - 1) {
                if (args[i + 1].matches("(-)?(\\d){1,5}")) {
                    port = Integer.parseInt(args[i + 1]);
                }
            }
        }

        // Setting up Database Connection
        this.database = new DatabaseConnection("127.0.0.1", 3306, "FuelPrices", "root", "");

        // Creating Parse Thread
        this.parser = new PageParser(delay);

        // Creating Client Managager
        this.connectionManager = new ConnectionManager(port);
    }

    public void start() {
        this.running = true;
        System.out.println("Starting Bot...");

        // Connecting to database
        database.connect();

        // Starting Parse Thread
        parser.start();

        // Starting Client Managager
        connectionManager.start();

        // Handle Console commands
        handleInput();
    }

    public void stop() {
        this.running = false;
        System.out.println("Stopping Bot...");

        // Disconnecting from database
        if (database.isConnected()) {
            database.disconnect();
        }

        // Shutting down Parse Thread
        parser.cancel();

        // Shutting down Client Manager
        connectionManager.cancel();

        System.out.println("Goodbye. See you soon! c:");
    }

    private void handleInput() {
        final Scanner scanner = new Scanner(System.in);

        String line;
        while (running && scanner.hasNextLine()) {
            line = scanner.nextLine();

            // Checking if the bot should quit
            if (line.toLowerCase().startsWith("exit")) {
                break;
            }

            // Executing the command
            final boolean result = Command.execute(line);

            // Checking if a command was found, otherwise send Help Message
            if (!result) {
                System.out.println("Unknown command! Type 'help' for help.");
            }
        }

        stop();
    }

    private void registerCommands() {
        // Register all commands
        Command.registerCommand(new AddPageCommand());
        Command.registerCommand(new DelayCommand());
        Command.registerCommand(new GraphCommand());
        Command.registerCommand(new HelpCommand());
        Command.registerCommand(new PrintCommand());
        Command.registerCommand(new RemovePageCommand());
        Command.registerCommand(new UpdateCommand());
    }

}