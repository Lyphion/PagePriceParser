package me.lyphium.pagepriceparser;

import me.lyphium.pagepriceparser.command.Command;
import me.lyphium.pagepriceparser.command.HelpCommand;

import java.util.Scanner;

public class Bot {

    private static Bot instance;

    private boolean running;

    public Bot() {
        instance = this;
    }

    public void init(String[] args) {
        registerCommands();
    }

    public void start() {
        this.running = true;

        handleInputs();
    }

    public void stop() {
        this.running = false;

        System.out.println("Exit Programm...");
    }

    private void handleInputs() {
        final Scanner scanner = new Scanner(System.in);

        String line;
        while (scanner.hasNext()) {
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
        Command.registerCommand(new HelpCommand());
    }

}