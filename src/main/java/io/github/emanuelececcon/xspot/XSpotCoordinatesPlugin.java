package io.github.emanuelececcon.xspot;

import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class XSpotCoordinatesPlugin extends JavaPlugin {

    private volatile LabelSettings settings;
    private MapWatcher watcher;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings(0);
        watcher = new MapWatcher(this);
        getServer().getPluginManager().registerEvents(watcher, this);
        watcher.start();
        getLogger().info("Labelling treasure and explorer maps: " + describe());
    }

    @Override
    public void onDisable() {
        if (watcher != null) {
            watcher.detachAll();
        }
    }

    LabelSettings settings() {
        return settings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadSettings(settings.version() + 1);
            watcher.resendAll();
            sender.sendMessage("XSpotCoordinates reloaded: " + describe());
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("XSpotCoordinates " + getPluginMeta().getVersion() + ": " + describe()
                    + "; " + watcher.labelledMaps() + " map(s) labelled since startup");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 && "reload".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("reload") : List.of();
    }

    private void loadSettings(int version) {
        settings = LabelSettings.load(getConfig(), version, getLogger());
    }

    private String describe() {
        String text = "style " + LabelSettings.name(settings.style())
                + ", layout " + LabelSettings.name(settings.layout())
                + ", corner " + LabelSettings.name(settings.corner());
        return settings.disabledMarkers().isEmpty() ? text : text + ", skipping " + String.join(", ", settings.disabledMarkers());
    }
}
