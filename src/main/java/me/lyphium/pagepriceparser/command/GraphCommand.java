package me.lyphium.pagepriceparser.command;

import me.lyphium.pagepriceparser.Bot;
import me.lyphium.pagepriceparser.database.DatabaseConnection;
import me.lyphium.pagepriceparser.parser.Fuel;
import me.lyphium.pagepriceparser.parser.PriceData;
import me.lyphium.pagepriceparser.utils.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandInfo(
        description = "Create a graph of the prices",
        shortUsage = "graph <id/name/fuel> <value> <file>",
        usage = "graph <id/name/fuel> <value> <file> [begin] [end] [pattern] [course/average <type>]"
)
public class GraphCommand extends Command {

    public static final int GRAPH_STEPS = 4;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    public GraphCommand() {
        super("graph");
    }

    @Override
    public boolean onCommand(String label, String[] args) {
        if (args.length < 3 || args.length > 8) {
            return false;
        }

        final DatabaseConnection database = Bot.getInstance().getDatabase();

        // Checking if the connection to the database is available, otherwise can't print Informations
        if (!database.isConnected()) {
            System.err.println("No connection available");
            return true;
        }

        final File file = new File(args[2]);
        try {
            // Check if path is valid
            file.getCanonicalPath();
        } catch (IOException e) {
            System.err.println("Invalid file path");
            return true;
        }

        Timestamp begin = new Timestamp(0);
        Timestamp end = new Timestamp(System.currentTimeMillis());

        // Parse begin and end time
        if (args.length > 3) {
            begin = Utils.toTimestamp(args[3]);

            if (begin == null) {
                System.err.println("Invalid time format: 'milliseconds' or 'yyyy-[m]m-[d]d hh:mm:ss'");
                return true;
            }

            if (args.length > 4) {
                end = Utils.toTimestamp(args[4]);

                if (begin == null) {
                    System.err.println("Invalid time format: 'milliseconds' or 'yyyy-[m]m-[d]d hh:mm:ss'");
                    return true;
                }
            }
        }

        // Handle each cathegory differently
        if (args[0].equalsIgnoreCase("id") || args[0].equalsIgnoreCase("name")) {
            final PriceData data;

            // Get PriceData based on the input id or name
            if (args[0].equalsIgnoreCase("id")) {
                if (args[1].matches("(\\d){1,4}")) {
                    // Parse id
                    final int id = Integer.parseUnsignedInt(args[1]);

                    // Get PriceData from database
                    data = database.getPriceData(id, begin, end);
                } else {
                    System.err.println("Invalid id format or to many digits");
                    return true;
                }
            } else {
                // Parse name
                final String name = args[1];

                // Get PriceData from database
                data = database.getMostSimilarPriceData(name, begin, end);
            }

            // Check if data was found
            if (data == null) {
                System.err.println("No price data found");
                return true;
            }

            // Price Informations by Fuel and Time
            final Map<Fuel, PriceMap> prices = data.getPrices();

            // Apply pattern if exists
            if (args.length > 5) {
                final String pattern = args[5];
                prices.keySet().removeIf(f -> !f.getName().matches(pattern));
            }


            String subTitle = null;
            SimpleDateFormat format = null;
            final List<Triple<String, PriceMap, Color>> info = new ArrayList<>();

            // Apply value mapping
            if (args.length > 6) {
                if (args[6].equalsIgnoreCase("course") && args.length == 7) {
                    // graph id 1 test 0 now .* course
                    long startTime = System.currentTimeMillis(), endTime = 0;

                    for (Entry<Fuel, PriceMap> entry : prices.entrySet()) {
                        final PriceMap map = entry.getValue();
                        final LongFunction<Float> func = createRegression(map);

                        final long fuelStartTime = map.getKey(0);
                        final long fuelEndTime = map.getKey(map.size() - 1);

                        final PriceMap updated = new PriceMap();
                        updated.put(fuelStartTime, func.apply(fuelStartTime));
                        updated.put(fuelEndTime, func.apply(fuelEndTime));

                        info.add(new Triple<>(entry.getKey().getName(), updated, entry.getKey().getColor()));

                        if (fuelStartTime < startTime) {
                            startTime = fuelStartTime;
                        }
                        if (fuelEndTime > endTime) {
                            endTime = fuelEndTime;
                        }
                    }

                    subTitle = "Verlauf: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                } else if (args[6].equalsIgnoreCase("average") && args.length == 8) {
                    final AverageType type = AverageType.getByName(args[7]);
                    if (type == null) {
                        final String types = Stream.of(AverageType.values()).map(AverageType::getName).collect(Collectors.joining(", "));
                        System.err.println("Unknown type. Please use: '" + types + "'");
                        return true;
                    }

                    long startTime = System.currentTimeMillis(), endTime = 0;

                    switch (type) {
                        // graph id 1 test 0 now .* average day
                        case DAY: case MONDAY: case TUESDAY: case WEDNESDAY: case THURSDAY: case FRIDAY: case SATURDAY: case SUNDAY:
                            format = new SimpleDateFormat("HH:mm:ss");
                            format.setTimeZone(TimeZone.getTimeZone("UTC"));

                            for (Entry<Fuel, PriceMap> entry : prices.entrySet()) {
                                final PriceMap map = entry.getValue();
                                final Pair<Long, Long> times = getStartEndTime(type.getWeekDay(), map);

                                final PriceMap updated = mapPriceMap(type.getWeekDay(), map, times.getFirst(), times.getSecond());
                                info.add(new Triple<>(entry.getKey().getName(), updated, entry.getKey().getColor()));

                                if (times.getFirst() < startTime) {
                                    startTime = times.getFirst();
                                }
                                if (times.getSecond() > endTime) {
                                    endTime = times.getSecond();
                                }
                            }

                            if (type == AverageType.DAY) {
                                subTitle = "Tagesdurchschnitt: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                            } else {
                                subTitle = type.getName() + "-Tagesdurchschnitt: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                            }
                            break;
                        case WEEK:
                            format = new SimpleDateFormat("EEEE HH:mm:ss");
                            format.setTimeZone(TimeZone.getTimeZone("UTC"));

                            for (Entry<Fuel, PriceMap> entry : prices.entrySet()) {
                                final PriceMap map = entry.getValue();
                                final Pair<Long, Long> times = getStartEndTime(type.getWeekDay(), map);

                                final PriceMap updated = new PriceMap();
                                for (int i = 2; i <= 8; i++) {
                                    final PriceMap temp = mapPriceMap((i + 1) % 7 - 1, map, times.getFirst(), times.getSecond());

                                    for (int j = 0; j < temp.size(); j++) {
                                        updated.put(temp.getKey(j) + (i + 2) * 86400000L, temp.get(j));
                                    }
                                }
                                updated.put(updated.getKey(updated.size() - 1), updated.get(0));
                                info.add(new Triple<>(entry.getKey().getName(), updated, entry.getKey().getColor()));

                                if (times.getFirst() < startTime) {
                                    startTime = times.getFirst();
                                }
                                if (times.getSecond() > endTime) {
                                    endTime = times.getSecond();
                                }
                            }

                            subTitle = "Wochendurchschnitt: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                            break;
                        default:
                            System.err.println("Unknown type");
                            return true;
                    }
                } else {
                    // Invalid operation
                    return false;
                }
            } else {
                format = null;

                // Map normal values
                for (Entry<Fuel, PriceMap> entry : prices.entrySet()) {
                    info.add(new Triple<>(entry.getKey().getName(), entry.getValue(), entry.getKey().getColor()));
                }
            }

            // Check if data exists
            if (info.isEmpty()) {
                System.err.println("No price data found");
                return true;
            }

            // Create an export image
            final GraphImage image = new GraphImage(data.getName(), info, file, format, subTitle);
            image.draw();
            image.export();
            image.free();
        } else if (args[0].equalsIgnoreCase("fuel")) {
            final Fuel fuel;

            // Parsing fuel as an id or name
            if (args[1].matches("(\\d)+")) {
                fuel = Fuel.getById(Integer.parseUnsignedInt(args[1]));
            } else {
                fuel = Fuel.getByName(args[1]);
            }

            // Check if input is valid
            if (fuel == null) {
                System.err.println("Invalid fuel");
                return true;
            }

            // Get List of PriceData based on the fuel
            final List<PriceData> data = database.getPriceData(fuel, begin, end);

            // Apply pattern if exists
            if (args.length > 5) {
                final String pattern = args[5];
                data.removeIf(s -> !s.getName().matches(pattern));
            }

            String subTitle = null;
            SimpleDateFormat format = null;
            final List<Triple<String, PriceMap, Color>> info = new ArrayList<>();

            // Apply value mapping
            if (args.length > 6) {
                if (args[6].equalsIgnoreCase("course") && args.length == 7) {
                    subTitle = "Verlauf";
                    long startTime = System.currentTimeMillis(), endTime = 0;

                    for (PriceData priceData : data) {
                        final PriceMap map = priceData.getPrices(fuel);
                        final LongFunction<Float> func = createRegression(map);

                        final long fuelStartTime = map.getKey(0);
                        final long fuelEndTime = map.getKey(map.size() - 1);

                        final PriceMap updated = new PriceMap();
                        updated.put(fuelStartTime, func.apply(fuelStartTime));
                        updated.put(fuelEndTime, func.apply(fuelEndTime));

                        info.add(new Triple<>(priceData.getName(), updated, priceData.getColor()));

                        if (fuelStartTime < startTime) {
                            startTime = fuelStartTime;
                        }
                        if (fuelEndTime > endTime) {
                            endTime = fuelEndTime;
                        }
                    }

                    subTitle = "Verlauf: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                } else if (args[6].equalsIgnoreCase("average") && args.length == 8) {
                    final AverageType type = AverageType.getByName(args[7]);
                    if (type == null) {
                        final String types = Stream.of(AverageType.values()).map(AverageType::getName).collect(Collectors.joining(", "));
                        System.err.println("Unknown type. Please use: '" + types + "'");
                        return true;
                    }

                    long startTime = System.currentTimeMillis(), endTime = 0;

                    switch (type) {
                        // graph id 1 test 0 now .* average day
                        case DAY: case MONDAY: case TUESDAY: case WEDNESDAY: case THURSDAY: case FRIDAY: case SATURDAY: case SUNDAY:
                            format = new SimpleDateFormat("HH:mm:ss");
                            format.setTimeZone(TimeZone.getTimeZone("UTC"));

                            for (PriceData priceData : data) {
                                final PriceMap map = priceData.getPrices(fuel);
                                final Pair<Long, Long> times = getStartEndTime(type.getWeekDay(), map);

                                final PriceMap updated = mapPriceMap(type.getWeekDay(), map, times.getFirst(), times.getSecond());
                                info.add(new Triple<>(priceData.getName(), updated, priceData.getColor()));

                                if (times.getFirst() < startTime) {
                                    startTime = times.getFirst();
                                }
                                if (times.getSecond() > endTime) {
                                    endTime = times.getSecond();
                                }
                            }

                            if (type == AverageType.DAY) {
                                subTitle = "Tagesdurchschnitt: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                            } else {
                                subTitle = type.getName() + "-Tagesdurchschnitt: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                            }
                            break;
                        case WEEK:
                            format = new SimpleDateFormat("EEEE dd HH:mm:ss");
                            format.setTimeZone(TimeZone.getTimeZone("UTC"));

                            for (PriceData priceData : data) {
                                final PriceMap map = priceData.getPrices(fuel);
                                final Pair<Long, Long> times = getStartEndTime(type.getWeekDay(), map);

                                final PriceMap updated = new PriceMap();
                                for (int i = 2; i <= 8; i++) {
                                    final PriceMap temp = mapPriceMap((i + 1) % 7 - 1, map, times.getFirst(), times.getSecond());

                                    for (int j = 0; j < temp.size(); j++) {
                                        updated.put(temp.getKey(j) + (i + 2) * 86400000L, temp.get(j));
                                    }
                                }
                                updated.put(updated.getKey(updated.size() - 1), updated.get(0));
                                info.add(new Triple<>(priceData.getName(), updated, priceData.getColor()));

                                if (times.getFirst() < startTime) {
                                    startTime = times.getFirst();
                                }
                                if (times.getSecond() > endTime) {
                                    endTime = times.getSecond();
                                }
                            }

                            subTitle = "Wochendurchschnitt: " + dateFormat.format(startTime) + " - " + dateFormat.format(endTime);
                            break;
                        default:
                            System.err.println("Unknown type");
                            return true;
                    }
                } else {
                    // Invalid operation
                    return false;
                }
            } else {
                format = null;

                // Map normal values
                for (PriceData priceData : data) {
                    info.add(new Triple<>(priceData.getName(), priceData.getPrices(fuel), priceData.getColor()));
                }
            }

            // Check if data exists
            if (info.isEmpty()) {
                System.err.println("No price data found");
                return true;
            }

            info.sort((i1, i2) -> i1.getFirst().compareToIgnoreCase(i2.getFirst()));

            // Create an export image
            final GraphImage image = new GraphImage(fuel.getName(), info, file, format, subTitle);
            image.draw();
            image.export();
            image.free();
        } else {
            return false;
        }

        System.gc();
        return true;
    }

    private PriceMap mapPriceMap(int day, PriceMap map, long startTime, long endTime) {
        final TimeZone tz = TimeZone.getDefault();
        final Calendar cal = Calendar.getInstance();

        final PriceMap updated = new PriceMap();
        final float[] values = new float[24 * GRAPH_STEPS];

        int i = 0;
        boolean daylightTime = tz.inDaylightTime(new Date(startTime));

        for (long t = startTime; t <= endTime; t += 3600000L / GRAPH_STEPS) {
            final int index = map.nearestIndexOf(t) - 1;
            final float value = map.get(index);

            if (day > 0) {
                cal.setTimeInMillis(t);
                if (cal.get(Calendar.DAY_OF_WEEK) != day) {
                    continue;
                }
            }

            values[i % (24 * GRAPH_STEPS)] += value;

            if (daylightTime != tz.inDaylightTime(new Date(t))) {
                daylightTime = !daylightTime;

                if (daylightTime) {
                    for (int j = 0; j < GRAPH_STEPS; j++) {
                        i++;
                        values[i % (24 * GRAPH_STEPS)] += value;
                    }
                } else {
                    i -= GRAPH_STEPS;
                }
            }
            i++;
        }

        final int count = i / (24 * GRAPH_STEPS);
        for (int j = 0; j < 24 * GRAPH_STEPS; j++) {
            updated.put(j * 3600000L / GRAPH_STEPS, values[j] / count);
        }
        updated.put(24 * 3600000L, values[0] / count);

        return updated;
    }

    private Pair<Long, Long> getStartEndTime(int day, PriceMap map) {
        final Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis(map.getKey(0));
        int weekDay = cal.get(Calendar.DAY_OF_WEEK);
        if (day > 0) {
            cal.add(Calendar.DATE, (7 + day - weekDay) % 7);
        } else if (day == 0) {
            cal.add(Calendar.DATE, 1);
        } else if (day == -1) {
            cal.add(Calendar.DATE, (9 - weekDay) % 7);
        }

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        final long startTime = cal.getTimeInMillis();

        cal.setTimeInMillis(map.getKey(map.size() - 1));
        weekDay = cal.get(Calendar.DAY_OF_WEEK);
        if (day > 0) {
            cal.add(Calendar.DATE, (-6 + day - weekDay) % 7 - 1);
        } else if (day == 0) {
            cal.add(Calendar.DATE, -1);
        } else if (day == -1) {
            cal.add(Calendar.DATE, (-5 - weekDay) % 7 - 1);
        }

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);

        final long endTime = cal.getTimeInMillis();

        return new Pair<>(startTime, endTime);
    }

    private LongFunction<Float> createRegression(PriceMap map) {
        final double meanX = mean(map.keySet());
        final double meanY = mean(map.values());

        final double m = covariance(map.keySet(), meanX, map.values(), meanY) / variance(map.keySet(), meanX);
        final double n = meanY - m * meanX;

        return l -> (float) (l * m + n);
    }

    private double mean(float[] values) {
        double sum = 0;
        for (float v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double mean(long[] values) {
        double sum = 0;
        for (long v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double covariance(long[] x, double meanX, float[] y, double meanY) {
        double c = 0;
        for (int i = 0; i < x.length; i++) {
            c += (x[i] - meanX) * (y[i] - meanY);
        }
        return c;
    }

    private double variance(long[] values, double mean) {
        double v = 0;
        for (long value : values) {
            v += (value - mean) * (value - mean);
        }
        return v;
    }

}