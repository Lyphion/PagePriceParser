package me.lyphium.pagepriceparser;

import me.lyphium.pagepriceparser.utils.PrettyPrintStream;

public class Main {

    public static void main(String[] args) {
        System.setOut(new PrettyPrintStream(System.out, "INFO"));
        System.setErr(new PrettyPrintStream(System.err, "ERROR"));

        final Bot bot = new Bot();
        bot.init(args);
        bot.start();
    }

}
