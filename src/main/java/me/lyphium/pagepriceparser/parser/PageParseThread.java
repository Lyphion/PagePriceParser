package me.lyphium.pagepriceparser.parser;

import lombok.Getter;
import lombok.Setter;
import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
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

                update();

                time = System.currentTimeMillis() - time;
                System.out.println("Finished: Updated the Prices (" + time + "ms)");

                time = delay - time;
                if (time > 0) {
                    Thread.sleep(time);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void update() {
        // TODO Get pages from database and update them
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
            switch (Utils.getDomain(doc.baseUri())) {
                case "clever-tanken.de":
                    final double dp = Double.parseDouble(doc.getElementById("current-price-1").html()) + 0.009;
                    final double bp = Double.parseDouble(doc.getElementById("current-price-2").html()) + 0.009;

                    prices.put(PriceType.DIESEL, Utils.round(dp, 3));
                    prices.put(PriceType.BENZIN, Utils.round(bp, 3));
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
                                prices.put(PriceType.DIESEL, Utils.round(price, 3));
                            } else if (name.equals("Shell Super FuelSave E10")) {
                                prices.put(PriceType.BENZIN, Utils.round(price, 3));
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

}
