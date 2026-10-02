package io.github.emanuelececcon.xspot;

import java.util.BitSet;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * Paints the coordinate tag over one map. The canvas is shared by every player and keeps its
 * pixels between renders, so the tag is only repainted after a config reload.
 */
final class CoordinatesRenderer extends MapRenderer {

    private static final int SIZE = LabelPainter.SIZE;

    private final XSpotCoordinatesPlugin plugin;
    private final Target target;
    private final BitSet painted = new BitSet(SIZE * SIZE);
    private int paintedVersion = -1;

    CoordinatesRenderer(XSpotCoordinatesPlugin plugin, Target target) {
        super(false);
        this.plugin = plugin;
        this.target = target;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        LabelSettings settings = plugin.settings();
        if (settings.version() == paintedVersion) {
            return;
        }
        paintedVersion = settings.version();

        // null hands the pixel back to the map underneath
        painted.stream().forEach(i -> canvas.setPixelColor(i % SIZE, i / SIZE, null));
        painted.clear();
        if (settings.disabledMarkers().contains(target.marker())) {
            return;
        }

        int blocksPerPixel = 1 << map.getScale().getValue();
        int markerX = Math.floorDiv(target.x() - map.getCenterX(), blocksPerPixel) + SIZE / 2;
        int markerY = Math.floorDiv(target.z() - map.getCenterZ(), blocksPerPixel) + SIZE / 2;
        LabelPainter.paint((x, y, color) -> {
            if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
                canvas.setPixelColor(x, y, color);
                painted.set(y * SIZE + x);
            }
        }, target, settings, markerX, markerY);
    }
}
