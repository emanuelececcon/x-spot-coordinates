package io.github.emanuelececcon.xspot;

import java.awt.Color;
import java.util.BitSet;
import java.util.List;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

/** Draws the coordinate tag onto a 128 x 128 map picture with the game's map font. */
final class LabelPainter {

    static final int SIZE = 128;

    private static final int MARGIN = 2;      // gap between the tag and the map edge
    private static final int PAD = 2;         // gap between the tag border and the text
    private static final int LINE = 9;        // line pitch for the two-line layout
    private static final int GLYPH_ROWS = 7;  // visible rows of a digit or capital

    // Map palette colours: SAND at high brightness, COLOR_BROWN at normal and lowest brightness.
    private static final Color SAND_HIGH = new Color(247, 233, 163);
    private static final Color BROWN_NORMAL = new Color(88, 66, 44);
    private static final Color BROWN_LOWEST = new Color(54, 40, 27);

    /** Receives the pixels of the tag; coordinates may fall outside the map and must be clipped. */
    interface Pixels {
        void set(int x, int y, Color color);
    }

    private LabelPainter() {
    }

    static List<String> lines(Target target, LabelSettings.Layout layout) {
        String x = "X " + target.x();
        String z = "Z " + target.z();
        String oneLine = x + " " + z;
        if (layout == LabelSettings.Layout.ONE_LINE && boxWidth(width(oneLine)) <= SIZE - 2 * MARGIN) {
            return List.of(oneLine);
        }
        return List.of(x, z);
    }

    /** Paints the tag for {@code target}; (markerX, markerY) is the marker's pixel on the map. */
    static void paint(Pixels pixels, Target target, LabelSettings settings, int markerX, int markerY) {
        List<String> lines = lines(target, settings.layout());
        int textWidth = lines.stream().mapToInt(LabelPainter::width).max().orElse(0);
        int w = boxWidth(textWidth);
        int h = (lines.size() - 1) * LINE + GLYPH_ROWS + 2 * PAD + 2;
        int[] origin = origin(settings.corner(), w, h, markerX, markerY);
        int bx = origin[0];
        int by = origin[1];

        Color fill = null;
        Color border = null;
        Color halo = null;
        Color text;
        switch (settings.style()) {
            case DARK -> {
                fill = BROWN_LOWEST;
                border = BROWN_NORMAL;
                text = SAND_HIGH;
            }
            case INK -> {
                halo = SAND_HIGH;
                text = BROWN_LOWEST;
            }
            default -> {
                fill = SAND_HIGH;
                border = BROWN_NORMAL;
                text = BROWN_LOWEST;
            }
        }

        if (fill != null) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                    pixels.set(bx + x, by + y, edge ? border : fill);
                }
            }
        }

        BitSet ink = new BitSet(SIZE * SIZE);
        for (int i = 0; i < lines.size(); i++) {
            int cx = bx + 1 + PAD;
            int cy = by + 1 + PAD + i * LINE;
            for (char c : lines.get(i).toCharArray()) {
                MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(c);
                if (sprite == null) {
                    continue;
                }
                for (int row = 0; row < sprite.getHeight(); row++) {
                    for (int col = 0; col < sprite.getWidth(); col++) {
                        if (sprite.get(row, col) && inside(cx + col, cy + row)) {
                            ink.set((cy + row) * SIZE + cx + col);
                        }
                    }
                }
                cx += sprite.getWidth() + 1;
            }
        }

        if (halo != null) {
            for (int i = ink.nextSetBit(0); i >= 0; i = ink.nextSetBit(i + 1)) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int x = i % SIZE + dx;
                        int y = i / SIZE + dy;
                        if (inside(x, y) && !ink.get(y * SIZE + x)) {
                            pixels.set(x, y, halo);
                        }
                    }
                }
            }
        }
        for (int i = ink.nextSetBit(0); i >= 0; i = ink.nextSetBit(i + 1)) {
            pixels.set(i % SIZE, i / SIZE, text);
        }
    }

    /** Width in map pixels, with the one-pixel gap the map font leaves between characters. */
    static int width(String s) {
        int width = 0;
        for (int i = 0; i < s.length(); i++) {
            MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(s.charAt(i));
            width += (sprite == null ? 0 : sprite.getWidth()) + (i > 0 ? 1 : 0);
        }
        return width;
    }

    private static int boxWidth(int textWidth) {
        return textWidth + 2 * PAD + 2;
    }

    private static boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE;
    }

    private static int[] origin(LabelSettings.Corner corner, int w, int h, int markerX, int markerY) {
        int[] topLeft = {MARGIN, MARGIN};
        int[] topRight = {SIZE - MARGIN - w, MARGIN};
        int[] bottomLeft = {MARGIN, SIZE - MARGIN - h};
        int[] bottomRight = {SIZE - MARGIN - w, SIZE - MARGIN - h};
        return switch (corner) {
            case TOP_LEFT -> topLeft;
            case TOP_RIGHT -> topRight;
            case BOTTOM_LEFT -> bottomLeft;
            case BOTTOM_RIGHT -> bottomRight;
            case AUTO -> {
                // The corner whose tag centre is farthest from the marker; ties keep the earlier corner.
                int[] best = bottomRight;
                double bestDistance = -1;
                for (int[] candidate : List.of(bottomRight, bottomLeft, topRight, topLeft)) {
                    double dx = candidate[0] + w / 2.0 - markerX;
                    double dy = candidate[1] + h / 2.0 - markerY;
                    double distance = dx * dx + dy * dy;
                    if (distance > bestDistance) {
                        bestDistance = distance;
                        best = candidate;
                    }
                }
                yield best;
            }
        };
    }
}
