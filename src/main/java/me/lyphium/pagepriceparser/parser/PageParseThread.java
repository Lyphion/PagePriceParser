package me.lyphium.pagepriceparser.parser;

import lombok.Getter;
import lombok.Setter;
import me.lyphium.pagepriceparser.Bot;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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

                System.out.println("Updating Prices...");

                // TODO Get pages from database and update them

                System.out.println("Finished: Updated Prices");

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

    private boolean updatePage(String url) {
        final Document doc = loadPage(url);
        final Map<PriceType, Double> prices = loadPrices(doc);

        if (prices == null) {
            return false;
        }

        // TODO Insert into database

        return true;
    }

    private Document loadPage(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<PriceType, Double> loadPrices(Document doc) {
        if (doc == null) {
            return null;
        }

        final Map<PriceType, Double> prices = new EnumMap<>(PriceType.class);

        try {
            switch (getDomain(doc.baseUri())) {
                case "clever-tanken.de":
                    final double dp = Double.parseDouble(doc.getElementById("current-price-1").html()) + 0.009;
                    final double bp = Double.parseDouble(doc.getElementById("current-price-2").html()) + 0.009;

                    prices.put(PriceType.DIESEL, round(dp, 3));
                    prices.put(PriceType.BENZIN, round(bp, 3));
                    break;
                case "find.shell.com":
                    final Element list = doc.selectFirst(".fuels");

                    for (Element element : list.select(".fuels__row")) {
                        final Element nameElement = element.selectFirst(".fuels__row-type");
                        if (nameElement != null) {
                            final String name = nameElement.html();
                            final String priceString = element.selectFirst(".fuels__row-price").html();
                            final double price = Double.parseDouble(priceString.split("/")[0].substring(1));

                            if (name.equals("Shell Diesel FuelSave")) {
                                prices.put(PriceType.DIESEL, round(price, 3));
                            } else if (name.equals("Shell Super FuelSave E10")) {
                                prices.put(PriceType.BENZIN, round(price, 3));
                            }
                        }
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Page can't be parsed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return prices;
    }

    private String getDomain(String url) {
        try {
            final String domain = new URI(url).getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private double round(double value, int decimal) {
        final double pow = Math.pow(10, decimal);
        return Math.round(value * pow) / pow;
    }

}
