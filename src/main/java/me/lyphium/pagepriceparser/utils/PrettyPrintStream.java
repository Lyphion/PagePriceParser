package me.lyphium.pagepriceparser.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;

public class PrettyPrintStream extends PrintStream {

    private static final Date DATE = new Date();
    private static final Object LOCK = new Object();

    private static PrettyPrintStream cur = null;
    private static boolean empty = true;

    private final String prefix;

    public PrettyPrintStream(OutputStream out, String prefix) {
        super(out);
        this.prefix = prefix;
    }

    @Override
    public void println() {
        println("");
    }

    @Override
    public void println(boolean x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(char x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(int x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(double x) {
        println(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        println(x == null ? "null" : new String(x));
    }

    @Override
    public void println(String x) {
        synchronized (LOCK) {
            super.println(x);
            empty = true;
            cur = null;
        }
    }

    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    @Override
    public void print(boolean b) {
        print(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        print(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        print(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        print(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        print(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        print(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        print(s == null ? "null" : new String(s));
    }

    @Override
    public void print(String s) {
        if (s == null)
            s = "null";

        synchronized (LOCK) {
            if (cur != null && cur != this) {
                super.write('\n');
                empty = true;
            }

            cur = this;

            final String[] split = s.split("(?<=\n)");
            final String prefix = getPrefix();

            if (split.length == 0) {
                if (empty) {
                    super.print(prefix);
                    empty = false;
                }
                return;
            }

            final StringBuilder builder = new StringBuilder();
            for (String line : split) {
                if (empty) {
                    builder.append(prefix).append(line);
                } else {
                    builder.append(line);
                }

                empty = !line.isEmpty() && line.charAt(line.length() - 1) == '\n';
            }

            if (empty) {
                cur = null;
            }

            super.print(builder.toString());
        }
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    public String getPrefix() {
        DATE.setTime(System.currentTimeMillis());
        return String.format("[%tT] [%s]: ", DATE, prefix);
    }

}