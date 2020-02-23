package me.lyphium.pagepriceparser.utils;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Utils {

    private final Map<String, Long> DELAY_TABLE = new HashMap<String, Long>() {{
        put("[\\d]+(?=s)", 1L);
        put("[\\d]+(?=m)", 60L);
        put("[\\d]+(?=h)", 3600L);
        put("[\\d]+(?=d)", 86400L);
    }};

    public long calculateDelay(String s) {
        try {
            long delay = 0;
            for (Entry<String, Long> entry : DELAY_TABLE.entrySet()) {
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

}