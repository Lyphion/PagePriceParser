package me.lyphium.pagepriceparser;

import lombok.Getter;
import me.lyphium.pagepriceparser.command.Command;
import me.lyphium.pagepriceparser.command.HelpCommand;
import me.lyphium.pagepriceparser.parser.PageParseThread;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bot {

    @Getter
    private static Bot instance;

    private static final Map<String, Long> DELAY_TABLE = new HashMap<String, Long>() {{
        put("[\\d]+(?=s)", 1L);
        put("[\\d]+(?=m)", 60L);
        put("[\\d]+(?=h)", 3600L);
        put("[\\d]+(?=d)", 86400L);
    }};

    @Getter
    private boolean running;
    private PageParseThread parser;

    public Bot() {
        instance = this;
    }

    public void init(String[] args) {
        registerCommands();

        long delay = 60 * 60 * 1000;
        for (int i = 0; i < args.length; i++) {
            final String part = args[i];
            if (part.equals("-d") && i < args.length - 1) {
                delay = calculateDelay(args[i + 1]);
            }
        }

        this.parser = new PageParseThread(delay);

        System.out.println("Checking Pages every " + (delay / 1000) + "sec");
    }

    public void start() {
        this.running = true;
        System.out.println("Starting Bot...");

        parser.start();

        handleInputs();
    }

    public void stop() {
        this.running = false;

        System.out.println("Stopping Bot...");
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

    private long calculateDelay(String s) {
        try {
            long delay = 0;
            for (Entry<String, Long> entry : DELAY_TABLE.entrySet()) {
                Pattern p = Pattern.compile(entry.getKey());
                Matcher m = p.matcher(s);

                if (m.find()) {
                    delay += Long.parseLong(s.substring(m.start(), m.end())) * entry.getValue();
                }
            }
            return delay * 1000;
        } catch (Exception e) {
            return 60 * 60 * 1000;
        }
    }

}