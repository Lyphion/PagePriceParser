package me.lyphium.pagepriceparser;

import lombok.Getter;
import me.lyphium.pagepriceparser.command.*;
import me.lyphium.pagepriceparser.connection.ConnectionManager;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.PageParseThread;
import me.lyphium.pagepriceparser.utils.Utils;

import java.util.Scanner;

@Getter
public class Bot {

    @Getter
    private static Bot instance;

    private boolean running;
    private PageParseThread parser;
    private DatabaseConnection database;
    private ConnectionManager connectionManager;

    public Bot() {
        instance = this;
    }

    public void init(String[] args) {
        registerCommands();

        long delay = 60 * 60 * 1000;
        int port = 14703;
        for (int i = 0; i < args.length; i++) {
            final String part = args[i];
            if (part.equals("-d") && i < args.length - 1) {
                delay = Utils.calculateDelay(args[i + 1]);
            }
            if (part.equals("-p") && i < args.length - 1) {
                if (args[i + 1].matches("(\\d){1,5}")) {
                    port = Integer.parseUnsignedInt(args[i + 1]);
                }
            }
        }

        this.database = new DatabaseConnection("127.0.0.1", 3306, "FuelPrices", "root", "");
        this.parser = new PageParseThread(delay);
        this.connectionManager = new ConnectionManager(port);

        System.out.println("Checking Pages every " + (delay / 1000) + "sec");
    }

    public void start() {
        this.running = true;
        System.out.println("Starting Bot...");

        database.connect();
        parser.start();
        connectionManager.start();

        handleInput();
    }

    public void stop() {
        this.running = false;
        System.out.println("Stopping Bot...");

        if (database.isConnected()) {
            database.disconnect();
        }
        parser.cancel();
        connectionManager.cancel();
    }

    private void handleInput() {
        final Scanner scanner = new Scanner(System.in);

        String line;
        while (running && scanner.hasNext()) {
            line = scanner.nextLine();

            if (line.toLowerCase().startsWith("exit")) {
                break;
            }

            final boolean result = Command.execute(line);
            if (!result) {
                System.out.println("Unknown command! Type 'help' for help.");
            }
        }

        stop();
    }

    private void registerCommands() {
        Command.registerCommand(new AddPageCommand());
        Command.registerCommand(new DelayCommand());
        Command.registerCommand(new HelpCommand());
        Command.registerCommand(new PrintCommand());
        Command.registerCommand(new RemovePageCommand());
        Command.registerCommand(new UpdateCommand());
    }

}