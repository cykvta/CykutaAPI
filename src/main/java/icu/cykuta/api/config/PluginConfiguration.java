package icu.cykuta.api.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * A {@link YamlConfiguration} whose typed getters <b>write back the default</b>
 * when a path is missing. This keeps config files self-healing: the first time a
 * plugin reads a new setting the key is created in memory, ready to be persisted
 * by {@link ConfigFile#save()}.
 */
public class PluginConfiguration extends YamlConfiguration {

    /**
     * Get the string at the path; if it doesn't exist, set it to {@code def} and return it.
     */
    @Override
    public String getString(@NotNull String path, String def) {
        if (!isSet(path)) {
            set(path, def);
            return def;
        }
        return super.getString(path, def);
    }

    /**
     * Get the string at the path, falling back to a "not found" marker.
     */
    @Override
    public String getString(@NotNull String path) {
        return getString(path, "No value found (" + this.getName() + ":" + path + ")");
    }

    /**
     * Get the int at the path; if it doesn't exist, set it to {@code def} and return it.
     */
    @Override
    public int getInt(@NotNull String path, int def) {
        if (!isSet(path)) {
            set(path, def);
            return def;
        }
        return super.getInt(path, def);
    }

    /**
     * Get the boolean at the path; if it doesn't exist, set it to {@code def} and return it.
     */
    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        if (!isSet(path)) {
            set(path, def);
            return def;
        }
        return super.getBoolean(path, def);
    }

    /**
     * Get the double at the path; if it doesn't exist, set it to {@code def} and return it.
     */
    @Override
    public double getDouble(@NotNull String path, double def) {
        if (!isSet(path)) {
            set(path, def);
            return def;
        }
        return super.getDouble(path, def);
    }

    /**
     * Get a {@link Material} at the path, defaulting to {@code BARRIER} when unset.
     */
    public Material getMaterial(@NotNull String path) {
        return Material.matchMaterial(getString(path, "BARRIER"));
    }

    /**
     * Load a configuration from a file. A missing file yields an empty configuration.
     */
    public static @NotNull PluginConfiguration loadConfiguration(@NotNull File file) {
        PluginConfiguration config = new PluginConfiguration();

        try {
            config.load(file);
        } catch (FileNotFoundException ignored) {
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load " + file, ex);
        }

        return config;
    }

    /**
     * Load a configuration from an input stream (e.g. a bundled resource).
     */
    public static @NotNull PluginConfiguration loadConfiguration(@NotNull InputStream is) {
        PluginConfiguration config = new PluginConfiguration();

        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            config.load(reader);
        } catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot load configuration from stream", ex);
        }

        return config;
    }
}
