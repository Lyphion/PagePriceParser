package me.lyphium.pagepriceparser.utils;

import lombok.experimental.UtilityClass;
import me.lyphium.pagepriceparser.parser.PageParser;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@UtilityClass
public class Utils {

    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

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
            return PageParser.DEFAULT_DELAY;
        }
    }

    public int distance(String from, String to) {
        final int fromLength = from.length() + 1;
        final int toLength = to.length() + 1;

        int[] cost = IntStream.range(0, fromLength).toArray();
        int[] newCost = new int[fromLength];

        for (int j = 1; j < toLength; j++) {
            newCost[0] = j;

            for (int i = 1; i < fromLength; i++) {
                final int match = from.charAt(i - 1) == to.charAt(j - 1) ? 0 : 2;

                final int replaceCost = cost[i - 1] + match;
                final int insertCost = cost[i] + 1;
                final int deleteCost = newCost[i - 1] + 1;

                newCost[i] = Math.min(Math.min(insertCost, deleteCost), replaceCost);
            }

            final int[] swap = cost;
            cost = newCost;
            newCost = swap;
        }

        return cost[fromLength - 1];
    }

    public String getDomain(String url) {
        try {
            final String domain = new URI(url).getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public float round(float value, int decimal) {
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
        try {
            if (s.equalsIgnoreCase("now")) {
                return new Timestamp(System.currentTimeMillis());
            } else if (s.matches("(-)?(\\d)+")) {
                return new Timestamp(Long.parseLong(s));
            }
            return Timestamp.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    public Color randomColor() {
        final Random random = new Random();

        Color color;
        do {
            color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        } while (0.21f * color.getRed() + 0.72f * color.getGreen() + 0.07f * color.getBlue() < 60f);

        return color;
    }

    public Color parseColor(String s) {
        final Color color;
        if (s.matches("(#)?[0-9a-fA-F]{6}")) {
            if (s.startsWith("#")) {
                color = Color.decode(s);
            } else {
                color = Color.decode('#' + s);
            }
        } else if (s.matches("(#)?[0-9a-fA-F]{3}")) {
            final char[] chars = s.toCharArray();
            if (s.startsWith("#")) {
                color = Color.decode("#" + chars[1] + chars[1] + chars[2] + chars[2] + chars[3] + chars[3]);
            } else {
                color = Color.decode("#" + chars[0] + chars[0] + chars[1] + chars[1] + chars[2] + chars[2]);
            }
        } else {
            return null;
        }
        return color;
    }

    public String toString(Date date) {
        return format.format(date);
    }

}