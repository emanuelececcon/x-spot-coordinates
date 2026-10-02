package io.github.emanuelececcon.xspot;

import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.FileConfiguration;

/** Label options from config.yml. Every reload makes a new instance with a higher version. */
record LabelSettings(Style style, Layout layout, Corner corner, Set<String> disabledMarkers, int version) {

    enum Style { PARCHMENT, INK, DARK }

    enum Layout { ONE_LINE, TWO_LINES }

    enum Corner { AUTO, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    static LabelSettings load(FileConfiguration config, int version, Logger log) {
        Set<String> disabled = config.getStringList("disabled-markers").stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT).replace("minecraft:", ""))
                .collect(Collectors.toUnmodifiableSet());
        return new LabelSettings(
                read(config, "style", Style.PARCHMENT, log),
                read(config, "layout", Layout.ONE_LINE, log),
                read(config, "corner", Corner.AUTO, log),
                disabled,
                version);
    }

    /** Config spelling of an enum value: TWO_LINES -> two-lines. */
    static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static <E extends Enum<E>> E read(FileConfiguration config, String key, E fallback, Logger log) {
        String raw = config.getString(key, name(fallback));
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            log.warning("config.yml: unknown " + key + " '" + raw + "', using " + name(fallback));
            return fallback;
        }
    }
}
