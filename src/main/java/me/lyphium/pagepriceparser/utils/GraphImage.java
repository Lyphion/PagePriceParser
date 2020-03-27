package me.lyphium.pagepriceparser.utils;

import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Getter
public class GraphImage {

    // Textalignment
    public enum Alignment {
        LEFT, CENTER, RIGHT
    }

    // Default Sizes and Positions
    public static final short
            WIDTH = 1920,
            HEIGHT = 1080,
            GRAPH_MIN_X = 70,
            GRAPH_MAX_X = WIDTH - 50,
            GRAPH_MIN_Y = 100,
            GRAPH_MAX_Y = HEIGHT - 130,
            INFO_LINE_Y = HEIGHT - 20,
            GRAPH_TIME_OFFSET = 62;

    // Default Graphlinestroke
    public static final BasicStroke LINE_STROKE = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    // Default Colors for the graph
    public static final Color
            ORANGE = new Color(211, 84, 0),
            SILVER = new Color(189, 195, 199),
            CONCRETE = new Color(149, 165, 166),
            ASBESTOS = new Color(127, 140, 141),
            LIGHT_GRAY = new Color(104, 104, 104),
            DARK_GRAY = new Color(25, 25, 25);

    // Custom fonts for the graph
    private static Font REGULAR, ITALIC, BOLD, BOLD_ITALIC;

    static {
        try {
            REGULAR = Font.createFont(Font.PLAIN, GraphImage.class.getResourceAsStream("/fonts/Roboto-Regular.ttf"));
            ITALIC = Font.createFont(Font.PLAIN, GraphImage.class.getResourceAsStream("/fonts/Roboto-Italic.ttf"));
            BOLD = Font.createFont(Font.PLAIN, GraphImage.class.getResourceAsStream("/fonts/Roboto-Bold.ttf"));
            BOLD_ITALIC = Font.createFont(Font.PLAIN, GraphImage.class.getResourceAsStream("/fonts/Roboto-BoldItalic.ttf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BufferedImage image;
    private final Graphics2D gc;
    private final Random random;

    private final String name;
    private final List<Triple<String, PriceMap, Color>> data;

    private File target;
    private float minValue = Float.MAX_VALUE, maxValue = Float.MIN_VALUE;
    private long minTime = Long.MAX_VALUE, maxTime = Long.MIN_VALUE;

    public GraphImage(String name, List<Triple<String, PriceMap, Color>> data, File target) {
        this.name = name;
        this.data = data;
        this.target = target;

        this.image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        this.gc = (Graphics2D) image.getGraphics();

        this.random = new Random(System.currentTimeMillis());

        // Calculate the minimum and maximum values for the x and y axis
        calcBorder();

        // Init the quality drawing settings
        gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        gc.setComposite(AlphaComposite.Src);
    }

    public void draw() {
        // Draw background
        drawRect(0, 0, WIDTH, HEIGHT, verticalGradient(DARK_GRAY, 0, HEIGHT), false);

        // Draw Title with Name and Time
        drawText(WIDTH / 2, 50, name, ORANGE, BOLD, 30, Alignment.CENTER);

        // Create and draw timespan
        final String times = Utils.toString(new Date(minTime)) + " Uhr - " + Utils.toString(new Date(maxTime)) + " Uhr";
        drawText(WIDTH / 2, 75, times, CONCRETE, ITALIC, 20, Alignment.CENTER);

        // Draw Timestamps
        final Date time = new Date();
        final long stepTime = (maxTime - minTime) / (GRAPH_MAX_X - GRAPH_MIN_X) * GRAPH_TIME_OFFSET;
        for (int x = GRAPH_MIN_X + GRAPH_TIME_OFFSET, i = 1; x <= GRAPH_MAX_X; x += GRAPH_TIME_OFFSET, i++) {
            time.setTime(minTime + stepTime * i);
            drawText(x, GRAPH_MAX_Y + 15, Utils.toString(time), ASBESTOS, REGULAR, 12, -Math.PI / 5, Alignment.RIGHT);
        }

        // Fixed stepValue
        final float minValue = 0.5f;
        final float maxValue = 2.0f;
        final float dif = maxValue - minValue;
        final float stepValue = 0.1f;
        final float stepPos = (GRAPH_MAX_Y - GRAPH_MIN_Y) / (dif / stepValue);

        // Draw horizontal lines and prices
        for (short i = 1; GRAPH_MAX_Y - i * stepPos > GRAPH_MIN_Y; i++) {
            // Calculate Value and Position
            final float value = minValue + i * stepValue;
            final int posY = (int) (GRAPH_MAX_Y - i * stepPos);

            // Draw horizontal line and price
            drawLine(GRAPH_MIN_X, posY, GRAPH_MAX_X, posY, 1, LIGHT_GRAY);
            drawText(GRAPH_MIN_X - 10, posY + 5, String.format("%.2f€", value), ASBESTOS, BOLD_ITALIC, 14, Alignment.RIGHT);
        }

        // Fill Graph
        final int infoOffset = (GRAPH_MAX_X - GRAPH_MIN_X) / data.size();
        for (int i = data.size() - 1; i >= 0; i--) {
            Triple<String, PriceMap, Color> entry = data.get(i);
            // Get Color of line
            final Color color = entry.getThird();

            // Map Graphpoints
            final int[] x = mapXValues(entry.getSecond().keySet(), minTime, maxTime);
            final int[] y = mapYValues(entry.getSecond().values(), minValue, maxValue);

            // Map Graphpoints with steps
//            final int[] tempX = mapXValues(entry.getValue().keySet(), minTime, maxTime);
//            final int[] tempY = mapYValues(entry.getValue().values(), minValue, maxValue);
//
//            final int[] x = new int[tempX.length * 2 - 1];
//            final int[] y = new int[tempY.length * 2 - 1];
//            createSteps(tempX, x, tempY, y);

            // Calculate Minimum and Maximum
            final int min = Arrays.stream(y).min().orElse(0);
            final int max = Arrays.stream(y).max().orElse(0);

            // Draw Graphline
            drawPolyline(x, y, LINE_STROKE, verticalGradient(color, min, max));

            // Draw Lineinfo
            drawRect(GRAPH_MIN_X + infoOffset * i + infoOffset / 4 - 10, INFO_LINE_Y - 10, 10, 10, color, false);
            drawText(GRAPH_MIN_X + infoOffset * i + infoOffset / 4 + 6, INFO_LINE_Y, entry.getFirst(), LIGHT_GRAY, ITALIC, 14, Alignment.LEFT);
        }

        // Draw Axis
        drawLine(GRAPH_MIN_X, GRAPH_MIN_Y - 10, GRAPH_MIN_X, GRAPH_MAX_Y, 3, SILVER);
        drawLine(GRAPH_MIN_X, GRAPH_MAX_Y, GRAPH_MAX_X + 10, GRAPH_MAX_Y, 3, SILVER);

        // Draw Arrows
        drawArrow(GRAPH_MIN_X, GRAPH_MIN_Y - 15, GRAPH_MIN_X - 5, GRAPH_MIN_Y - 5, GRAPH_MIN_X + 5, GRAPH_MIN_Y - 5, 3, SILVER);
        drawArrow(GRAPH_MAX_X + 15, GRAPH_MAX_Y, GRAPH_MAX_X + 5, GRAPH_MAX_Y - 5, GRAPH_MAX_X + 5, GRAPH_MAX_Y + 5, 3, SILVER);
    }

    private void drawRect(int posX, int posY, int width, int height, Paint paint, boolean center) {
        gc.setPaint(paint);

        if (center) {
            gc.fillRect(posX - width / 2, posY - height / 2, width, height);
        } else {
            gc.fillRect(posX, posY, width, height);
        }
    }

    private void drawLine(int startX, int startY, int endX, int endY, float width, Paint paint) {
        gc.setPaint(paint);
        gc.setStroke(new BasicStroke(width));

        gc.drawLine(startX, startY, endX, endY);
    }

    private void drawPolyline(int[] xPoints, int[] yPoints, Stroke stroke, Paint paint) {
        gc.setStroke(stroke);
        gc.setPaint(paint);

        gc.drawPolyline(xPoints, yPoints, Math.min(xPoints.length, yPoints.length));
    }

    private void drawText(int posX, int posY, String text, Paint paint, Font font, float size, Alignment alignment) {
        gc.setPaint(paint);
        gc.setFont(font.deriveFont(size));

        final FontMetrics metrics = gc.getFontMetrics();
        final int width = metrics.stringWidth(text);

        if (alignment == Alignment.LEFT) {
            gc.drawString(text, posX, posY);
        } else if (alignment == Alignment.CENTER) {
            gc.drawString(text, posX - width / 2f, posY);
        } else if (alignment == Alignment.RIGHT) {
            gc.drawString(text, posX - width, posY);
        }
    }

    private void drawText(int posX, int posY, String text, Paint paint, Font font, float size, double angle, Alignment alignment) {
        final AffineTransform original = gc.getTransform();
        gc.translate(posX, posY);
        gc.rotate(angle);

        drawText(0, 0, text, paint, font, size, alignment);

        gc.setTransform(original);
    }

    private void drawArrow(int centerX, int centerY, int edgeX1, int edgeY1, int edgeX2, int edgeY2, int width, Paint paint) {
        drawPolyline(
                new int[]{edgeX1, centerX, edgeX2},
                new int[]{edgeY1, centerY, edgeY2},
                new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER),
                paint
        );
    }

    private GradientPaint verticalGradient(Color color, int minY, int maxY) {
        return new GradientPaint(0, minY, color.brighter(), 0, maxY, color.darker());
    }

    private int calcYPos(float value, float min, float max) {
        return GRAPH_MAX_Y - (int) ((value - min) * (GRAPH_MAX_Y - GRAPH_MIN_Y) / (max - min));
    }

    private int calcXPos(long value, long min, long max) {
        return (int) ((value - min) * (GRAPH_MAX_X - GRAPH_MIN_X) / (max - min)) + GRAPH_MIN_X;
    }

    private int[] mapYValues(float[] in, float min, float max) {
        final int[] out = new int[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = calcYPos(in[i], min, max);
        }
        return out;
    }

    private int[] mapXValues(long[] in, long min, long max) {
        final int[] out = new int[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = calcXPos(in[i], min, max);
        }
        return out;
    }

    private void calcBorder() {
        for (Triple<String, PriceMap, Color> entry : data) {
            final PriceMap map = entry.getSecond();

            if (map.isEmpty()) {
                continue;
            }

            // Get first and last key in map (Map is sorted -> minimum and maximum)
            final long min = map.getKey(0);
            final long max = map.getKey(map.size() - 1);

            if (min < minTime) {
                minTime = min;
            }
            if (max > maxTime) {
                maxTime = max;
            }

            // Get minimum and maximum values in map
            for (int i = 0; i < map.size(); i++) {
                final float value = map.get(i);
                if (value < minValue) {
                    minValue = value;
                }
                if (value > maxValue) {
                    maxValue = value;
                }
            }
        }
    }

    private void createSteps(int[] timeIn, int[] timeOut, int[] valueIn, int[] valueOut) {
        /*
         *      +---+
         *      |   |     +---+
         *   +--+   |     |
         *          +-----+
         */

        for (int i = 0; i < timeIn.length; i++) {
            timeOut[i * 2] = timeIn[i];
            if (i < timeIn.length - 1) {
                timeOut[i * 2 + 1] = timeIn[i + 1];
            }
        }
        for (int i = 0; i < valueIn.length; i++) {
            valueOut[i * 2] = valueIn[i];
            if (i < valueIn.length - 1) {
                valueOut[i * 2 + 1] = valueIn[i];
            }
        }
    }

    public void export() {
        try {
            // Delete old file if it exists
            if (target.exists()) {
                target.delete();
            }

            // Parse file format, default 'png'
            final String format;
            final int index = target.getName().lastIndexOf('.');
            if (index > -1) {
                format = target.getName().substring(index + 1);
            } else {
                format = "png";
                target = new File(target.getParent(), target.getName() + ".png");
            }

            // Write file to drive
            final boolean result = ImageIO.write(image, format, target);

            // Check if the file couldn't be exported
            if (result) {
                System.out.println("File exported to: " + target.getAbsolutePath());
            } else {
                System.err.println("File couldn't be exported: " + target.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void free() {
        gc.dispose();
        image.flush();
        target = null;
    }

}