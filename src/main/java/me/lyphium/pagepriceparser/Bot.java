package me.lyphium.pagepriceparser;

import lombok.Getter;
import me.lyphium.pagepriceparser.command.Command;
import me.lyphium.pagepriceparser.command.DelayCommand;
import me.lyphium.pagepriceparser.command.HelpCommand;
import me.lyphium.pagepriceparser.command.UpdateCommand;
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

    public Bot() {
        instance = this;
    }

    public void init(String[] args) {
        registerCommands();

        long delay = 60 * 60 * 1000;
        for (int i = 0; i < args.length; i++) {
            final String part = args[i];
            if (part.equals("-d") && i < args.length - 1) {
                delay = Utils.calculateDelay(args[i + 1]);
            }
        }

        this.database = new DatabaseConnection("127.0.0.1", 3306, "FuelPrices", "root", "");
        this.parser = new PageParseThread(delay);

        System.out.println("Checking Pages every " + (delay / 1000) + "sec");
    }

    public void start() {
        this.running = true;
        System.out.println("Starting Bot...");

        database.connect();
        parser.start();

        handleInput();
    }

    public void stop() {
        this.running = false;

        if (database.isConnected()) {
            database.disconnect();
        }
        parser.cancel();

        System.out.println("Stopping Bot...");
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
        Command.registerCommand(new DelayCommand());
        Command.registerCommand(new HelpCommand());
        Command.registerCommand(new UpdateCommand());
    }

}