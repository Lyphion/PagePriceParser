package me.lyphium.pagepriceparser.parser;

import lombok.Getter;
import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.utils.PriceMap;
import me.lyphium.pagepriceparser.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PageParser {

    public static final long DEFAULT_PERIOD = 60 * 60 * 1000;

    @Getter
    private final long period;
    @Getter
    private final long startTime;

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    public PageParser(long period, long startTime) {
        this.period = period;
        this.startTime = startTime;
    }

    public void start() {
        if (period < 0) {
            System.out.println("Automatic Page Parser disabled");
            return;
        }

        System.out.println("Started Page Parser");
        if (period < 1000) {
            System.out.println("Checking Pages every " + period + "ms");
        } else {
            System.out.println("Checking Pages every " + (period / 1000) + "sec");
        }

        final long startDelay;
        if (startTime > System.currentTimeMillis()) {
            System.out.println("First Check: " + Utils.toString(new Date(startTime)));
            startDelay = startTime - System.currentTimeMillis();
        } else {
            startDelay = 0;
        }

        service.scheduleAtFixedRate(
                this::update,
                startDelay,
                period,
                TimeUnit.MILLISECONDS
        );
    }

    public synchronized void update() {
        long time = System.currentTimeMillis();

        try {
            // Checking if the connection to the database is available, otherwise don't update prices
            if (!Bot.getInstance().getDatabase().isConnected()) {
                System.err.println("Can't update database! No connection available");
                return;
            }

            System.out.println("Updating Prices...");

            // Update prices
            handleUpdate();

            time = System.currentTimeMillis() - time;
            System.out.println("Finished: Updated the Prices (" + time + "ms)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void handleUpdate() {
        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available
        if (!database.isConnected()) {
            System.err.println("Can't update database! No connection available");
            return;
        }

        // All available pages
        final List<PriceData> pages = database.getPages();
        final long time = System.currentTimeMillis();

        // Nothing to update if no pages are available
        if (pages.isEmpty()) {
            return;
        }

        pages.parallelStream().forEach(page -> {
            // Load HTML-Page
            final Document doc = loadPage(page.getUrl());

            // Load Prices from page
            final Map<Fuel, Float> prices = loadPrices(doc);

            // Check if prices exists (HTML-Page correct and prices exist)
            if (prices == null) {
                System.err.println("Couldn't update prices for: " + page.getName());
                return;
            }

            // Apply prices to PriceData Object
            for (Entry<Fuel, Float> entry : prices.entrySet()) {
                page.getPrices().put(
                        entry.getKey(),
                        new PriceMap(new long[]{time}, new float[]{entry.getValue()})
                );
            }
        });

        // Save prices in database
        database.savePriceData(pages);
        System.gc();
    }

    public synchronized void cancel() {
        service.shutdown();

        if (period < 0) {
            return;
        }

        System.out.println("Shut down Page Parser");
    }

    private Document loadPage(String url) {
        try {
            // Load HTML-Page
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
            // Parsing pages based on the domain
            switch (Utils.getDomain(doc.baseUri())) {
                case "clever-tanken.de":
                    final Element container = doc.selectFirst("#prices-container");

                    for (Element row : container.select(".price-row")) {
                        try {
                            final String name = row.selectFirst(".price-type-name").html();
                            final Fuel fuel = Fuel.getByName(name);
                            if (fuel == null) {
                                continue;
                            }

                            final Element priceField = row.selectFirst(".price-field");
                            final String priceString = priceField.selectFirst("span").html();

                            final float price = Float.parseFloat(priceString) + 0.009F;
                            final float value = Utils.round(price, 3);

                            prices.put(fuel, value);
                        } catch (Exception e) {
                            System.err.println("Error while parsing: " + doc.baseUri());
                        }
                    }

                    return prices;
                case "find.shell.com":
                    final Element list = doc.selectFirst(".fuels");

                    for (Element element : list.select(".fuels__row")) {
                        try {
                            final Element nameElement = element.selectFirst(".fuels__row-type");
                            if (nameElement == null) {
                                continue;
                            }

                            final String name = nameElement.html();
                            final Fuel fuel = Fuel.getByName(name);
                            if (fuel == null) {
                                continue;
                            }

                            final Element priceField = element.selectFirst(".fuels__row-price");
                            final String priceString = priceField.html().split("/", 2)[0].substring(1);

                            final float price = Float.parseFloat(priceString);
                            final float value = Utils.round(price, 3);

                            prices.put(fuel, value);
                        } catch (Exception e) {
                            System.err.println("Error while parsing: " + doc.baseUri());
                        }
                    }

                    return prices;
                default:
                    System.err.println("Can't parse page: " + doc.baseUri());
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
