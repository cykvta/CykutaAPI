package icu.cykuta.api.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of a plugin's YAML files.
 * <p>
 * Register each file once (typically in {@code onEnable}), then read, save and
 * reload them by name:
 * <pre>{@code
 * ConfigManager configs = new ConfigManager(this);
 * configs.register("config.yml");
 * configs.register("lang.yml");
 *
 * String prefix = configs.get("lang.yml").getString("prefix");
 * configs.reloadAll(); // e.g. from a /reload command
 * }</pre>
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, ConfigFile> files = new LinkedHashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Register and load a file. If a resource with the same name is bundled in the
     * plugin jar it is used as the default; otherwise an empty file is created.
     *
     * @return the registered {@link ConfigFile}, already loaded.
     */
    public ConfigFile register(String fileName) {
        ConfigFile file = new ConfigFile(plugin, fileName);
        file.register();
        files.put(fileName, file);
        return file;
    }

    /**
     * Get the loaded configuration of a registered file.
     *
     * @throws IllegalArgumentException if the file was never registered.
     */
    public PluginConfiguration get(String fileName) {
        return getFile(fileName).getConfiguration();
    }

    /**
     * Get a registered {@link ConfigFile}.
     *
     * @throws IllegalArgumentException if the file was never registered.
     */
    public ConfigFile getFile(String fileName) {
        ConfigFile file = files.get(fileName);
        if (file == null) {
            throw new IllegalArgumentException("Config not registered: " + fileName);
        }
        return file;
    }

    /**
     * Save a single registered file to disk.
     */
    public void save(String fileName) {
        getFile(fileName).save();
    }

    /**
     * Reload a single registered file from disk.
     */
    public void reload(String fileName) {
        getFile(fileName).reload();
    }

    /**
     * Save every registered file to disk.
     */
    public void saveAll() {
        files.values().forEach(ConfigFile::save);
    }

    /**
     * Reload every registered file from disk.
     */
    public void reloadAll() {
        files.values().forEach(ConfigFile::reload);
    }
}
