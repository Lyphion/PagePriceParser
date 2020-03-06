package me.lyphium.pagepriceparser.utils;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Utils {

    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.YYYY HH:mm:ss");

    private final Map<String, Long> delayTable = new HashMap<String, Long>() {{
        put("[\\d]+(?=s)", 1L);
        put("[\\d]+(?=m)", 60L);
        put("[\\d]+(?=h)", 3600L);
        put("[\\d]+(?=d)", 86400L);
    }};

    public long calculateDelay(String s) {
        try {
            if (s.matches("(-)?(\\d)+")) {
                return Long.parseLong(s);
            }

            long delay = 0;
            for (Entry<String, Long> entry : delayTable.entrySet()) {
                Pattern p = Pattern.compile(entry.getKey());
                Matcher m = p.matcher(s);

                if (m.find()) {
                    delay += Long.parseUnsignedLong(s.substring(m.start(), m.end())) * entry.getValue();
                }
            }
            return delay * 1000;
        } catch (Exception e) {
            return 60 * 60 * 1000;
        }
    }

    public String getDomain(String url) {
        try {
            final String domain = new URI(url).getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public float round(double value, int decimal) {
        final double pow = Math.pow(10, decimal);
        return (float) (Math.round(value * pow) / pow);
    }

    public String[] wordWrap(String input, int width) {
        if (width < 1) {
            width = 1;
        }

        if (input == null) {
            return null;
        } else if (input.trim().length() <= width) {
            return new String[]{input.trim()};
        }

        final String[] words = input.trim().split("\\s");
        final List<String> lines = new ArrayList<>();
        final StringBuilder buf = new StringBuilder();

        for (String word : words) {
            if (buf.length() + 1 + word.length() > width) {
                final String line = buf.toString().trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
                buf.setLength(0);
            }
            buf.append(word).append(" ");
        }

        final String line = buf.toString().trim();
        if (!line.isEmpty()) {
            lines.add(line);
        }

        return lines.toArray(new String[0]);
    }

    public Timestamp toTimestamp(String s) {
        if (s.matches("(\\d)+")) {
            return new Timestamp(Long.parseUnsignedLong(s));
        }
        try {
            return Timestamp.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    public String toString(Date date) {
        return format.format(date);
    }

}