package io.github.emanuelececcon.xspot;

import io.papermc.paper.datacomponent.item.MapDecorations;

/**
 * The spot a treasure or explorer map points at, taken from the item's map_decorations component.
 * For a Buried Treasure Map this is the column of the buried chest.
 */
record Target(String marker, int x, int z) {

    /** The first decoration on the item, or null when it has none. */
    static Target of(MapDecorations decorations) {
        for (MapDecorations.DecorationEntry entry : decorations.decorations().values()) {
            return new Target(entry.type().getKey().getKey(), (int) Math.floor(entry.x()), (int) Math.floor(entry.z()));
        }
        return null;
    }
}
