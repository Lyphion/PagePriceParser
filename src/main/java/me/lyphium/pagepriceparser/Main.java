package me.lyphium.pagepriceparser;

import me.lyphium.pagepriceparser.utils.PrettyPrintStream;

public class Main {

    public static void main(String[] args) {
        // Changing Console Logformat
        System.setOut(new PrettyPrintStream(System.out, "INFO"));
        System.setErr(new PrettyPrintStream(System.err, "ERROR"));

        // Creating and starting Bot
        final Bot bot = new Bot();
        bot.init(args);
        bot.start();
    }

}
