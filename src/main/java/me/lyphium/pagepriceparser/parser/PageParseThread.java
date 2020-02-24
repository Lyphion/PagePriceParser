package me.lyphium.pagepriceparser.parser;

import lombok.Getter;
import lombok.Setter;
import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

                if (!Bot.getInstance().getDatabase().isConnected()) {
                    System.err.println("Can't update database! No connection available");

                    time = delay;
                } else {
                    System.out.println("Updating Prices...");

                    update();

                    time = System.currentTimeMillis() - time;
                    System.out.println("Finished: Updated the Prices (" + time + "ms)");

                    time = delay - time;
                }

                if (time > 0) {
                    Thread.sleep(time);
                }
            } catch (InterruptedException e) {
                // Thrown when PageParseThread is shutting down
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void update() {
        final DatabaseConnection database = Bot.getInstance().getDatabase();

        if (!database.isConnected()) {
            System.err.println("Can't update database! No connection available");
            return;
        }

        final List<PriceData> pages = database.getPages();

        pages.parallelStream().forEach(page -> {
            final Document doc = loadPage(page.getUrl());
            final Map<Fuel, Float> prices = loadPrices(doc);

            if (prices == null) {
                System.err.println("Couldn't update prices for: " + page.getName());
                return;
            }

            final long time = System.currentTimeMillis();
            for (Entry<Fuel, Float> entry : prices.entrySet()) {
                page.getPrices().put(
                        entry.getKey(),
                        Collections.singletonMap(time, entry.getValue())
                );
            }
        });

        database.savePriceData(pages);
    }

    public synchronized void cancel() {
        interrupt();
    }

    private Document loadPage(String url) {
        try {
            return Jsoup.connect(url).get();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<Fuel, Float> loadPrices(Document doc) {
        if (doc == null) {
            return null;
        }

        final Map<Fuel, Float> prices = new EnumMap<>(Fuel.class);

        try {
            switch (Utils.getDomain(doc.baseUri())) {
                case "clever-tanken.de":
                    final float dp = Float.parseFloat(doc.getElementById("current-price-1").html()) + 0.009F;
                    final float bp = Float.parseFloat(doc.getElementById("current-price-2").html()) + 0.009F;

                    prices.put(Fuel.DIESEL, Utils.round(dp, 3));
                    prices.put(Fuel.BENZIN, Utils.round(bp, 3));
                    break;
                case "find.shell.com":
                    final Element list = doc.selectFirst(".fuels");

                    for (Element element : list.select(".fuels__row")) {
                        final Element nameElement = element.selectFirst(".fuels__row-type");
                        if (nameElement != null) {
                            final String name = nameElement.html();
                            final String priceString = element.selectFirst(".fuels__row-price").html();
                            final float price = Float.parseFloat(priceString.split("/")[0].substring(1));

                            if (name.equals("Shell Diesel FuelSave")) {
                                prices.put(Fuel.DIESEL, Utils.round(price, 3));
                            } else if (name.equals("Shell Super FuelSave E10")) {
                                prices.put(Fuel.BENZIN, Utils.round(price, 3));
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
