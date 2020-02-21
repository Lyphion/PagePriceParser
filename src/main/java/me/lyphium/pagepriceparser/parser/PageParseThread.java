package me.lyphium.pagepriceparser.parser;

import lombok.Getter;
import lombok.Setter;
import me.lyphium.pagepriceparser.Bot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumMap;
import java.util.Map;

public class PageParseThread extends Thread {

    @Getter
    @Setter
    private long delay;

    public PageParseThread(long delay) {
        this.delay = delay;

        setName("PageParser");
        setDaemon(true);
    }

    @Override
    public void run() {
        while (Bot.getInstance().isRunning()) {
            try {
                long time = System.currentTimeMillis();

                // TODO Get pages from database
                // TODO Load and parse all pages
                // TODO Save result into database

                time = delay - (System.currentTimeMillis() - time);
                if (time > 0) {
                    Thread.sleep(time);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void cancel() {
        this.interrupt();
    }

    private Document loadPage(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<PriceType, Float> loadPrices(Document doc) {
        if (doc == null) {
            return null;
        }

        final Map<PriceType, Float> map = new EnumMap<>(PriceType.class);

        switch (getDomain(doc.baseUri())) {
            case "clever-tanken.de":
                break;
            case "shell.de":
                break;
            default:
                throw new UnsupportedOperationException("Page can't be parsed");
        }

        return map;
    }

    private String getDomain(String url) {
        try {
            final String domain = new URI(url).getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            return null;
        }
    }

}
