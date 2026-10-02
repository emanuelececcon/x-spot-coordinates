package io.github.emanuelececcon.xspot;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.MapDecorations;
import io.papermc.paper.datacomponent.item.MapId;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

/**
 * Finds treasure and explorer maps and puts a {@link CoordinatesRenderer} on each one.
 *
 * <p>Renderers live only in memory, so every map is found again after a restart: in players'
 * inventories (on join, on inventory events and by a light periodic scan) and in item frames
 * (when their chunk loads). The game only sends the parts of a map that changed, so a map that
 * players may already have received is re-sent in full once its renderer is added.
 */
final class MapWatcher implements Listener {

    private static final long SCAN_PERIOD_TICKS = 5;
    private static final int FULL_SCAN_EVERY = 8; // hands every 5 ticks, whole inventory every 2 seconds

    private final XSpotCoordinatesPlugin plugin;
    private final Map<Integer, CoordinatesRenderer> renderers = new HashMap<>();
    private int scans;

    MapWatcher(XSpotCoordinatesPlugin plugin) {
        this.plugin = plugin;
    }

    void start() {
        // Covers a late load (plugin manager or /reload): frames and players that are already here.
        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                inspect(frame.getItem());
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            inspectInventory(player);
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::scan, SCAN_PERIOD_TICKS, SCAN_PERIOD_TICKS);
    }

    int labelledMaps() {
        return renderers.size();
    }

    /** Pushes every labelled map to every player again, after the settings changed. */
    void resendAll() {
        for (Integer id : renderers.keySet()) {
            MapView view = Bukkit.getMap(id);
            if (view != null) {
                resend(view);
            }
        }
    }

    /** Takes the renderers off again, so a disabled plugin leaves vanilla maps behind. */
    void detachAll() {
        for (Map.Entry<Integer, CoordinatesRenderer> entry : renderers.entrySet()) {
            MapView view = Bukkit.getMap(entry.getKey());
            if (view != null) {
                view.removeRenderer(entry.getValue());
                if (!Bukkit.isStopping()) {
                    resend(view);
                }
            }
        }
        renderers.clear();
    }

    private void scan() {
        boolean full = ++scans % FULL_SCAN_EVERY == 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (full) {
                inspectInventory(player);
            } else {
                inspect(player.getInventory().getItemInMainHand());
                inspect(player.getInventory().getItemInOffHand());
            }
        }
    }

    private void inspectInventory(HumanEntity player) {
        for (ItemStack item : player.getInventory().getContents()) {
            inspect(item);
        }
        inspect(player.getItemOnCursor());
    }

    /**
     * Adds the renderer if {@code item} is a map with a marker that has none yet. Goes by the
     * components, not the item type: since 26.x every treasure and explorer map is its own item
     * (buried_treasure_map, woodland_mansion_map, ...), not a filled_map.
     */
    private void inspect(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return;
        }
        MapId mapId = item.getData(DataComponentTypes.MAP_ID);
        if (mapId == null || renderers.containsKey(mapId.id())) {
            return;
        }
        MapDecorations decorations = item.getData(DataComponentTypes.MAP_DECORATIONS);
        Target target = decorations == null ? null : Target.of(decorations);
        if (target == null) {
            return;
        }
        MapView view = Bukkit.getMap(mapId.id());
        if (view == null) {
            return;
        }
        CoordinatesRenderer renderer = new CoordinatesRenderer(plugin, target);
        view.addRenderer(renderer);
        renderers.put(mapId.id(), renderer);
        resend(view);
    }

    private static void resend(MapView view) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMap(view);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        inspectInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        inspect(event.getPlayer().getInventory().getItem(event.getNewSlot()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        inspect(event.getMainHandItem());
        inspect(event.getOffHandItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            inspect(event.getItem().getItemStack());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        inspect(event.getCurrentItem());
        inspect(event.getCursor());
        if (event.getHotbarButton() >= 0) {
            inspect(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFramePlace(PlayerItemFrameChangeEvent event) {
        inspect(event.getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof ItemFrame frame) {
                inspect(frame.getItem());
            }
        }
    }
}
