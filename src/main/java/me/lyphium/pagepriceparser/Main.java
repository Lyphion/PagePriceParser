package me.lyphium.pagepriceparser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import me.lyphium.pagepriceparser.utils.PrettyPrintStream;
import org.slf4j.LoggerFactory;

public class Main {

    public static void main(String[] args) {
        // Changing Console Logformat
        System.setOut(new PrettyPrintStream(System.out, "INFO"));
        System.setErr(new PrettyPrintStream(System.err, "ERROR"));

        // Disable Debug Log
        final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);

        // Creating and starting Bot
        final Bot bot = new Bot();
        bot.init(args);
        bot.start();
    }

}
